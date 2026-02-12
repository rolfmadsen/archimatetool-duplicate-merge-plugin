package com.archimatetool.merge;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.editor.model.commands.DeleteArchimateElementCommand;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IProperty;


/**
 * Merge Command
 */
public class MergeCommand extends CompoundCommand {
    
    private IArchimateElement fTarget;
    private IArchimateElement fSource;
    private boolean fMergeProperties;
    
    public MergeCommand(IArchimateElement target, IArchimateElement source, boolean mergeProperties) {
        setLabel("Merge Elements");
        
        fTarget = target;
        fSource = source;
        fMergeProperties = mergeProperties;
        
        // 1. Re-assign Source Relationships
        // Use a copy list to avoid ConcurrentModificationException if the list changes
        for(IArchimateRelationship rel : new ArrayList<>(fSource.getSourceRelationships())) {
            add(new ReconnectRelationshipCommand(rel, fTarget, true));
        }
        
        // 2. Re-assign Target Relationships
        for(IArchimateRelationship rel : new ArrayList<>(fSource.getTargetRelationships())) {
            add(new ReconnectRelationshipCommand(rel, fTarget, false));
        }
        
        // 3. Update Diagram Objects
        for(IDiagramModelArchimateObject dmo : new ArrayList<>(fSource.getReferencingDiagramObjects())) {
            add(new SetArchimateElementCommand(dmo, fTarget));
        }
        
        // 4. Merge Properties (Logical Level)
        if(fMergeProperties) {
            add(new MergePropertiesCommand(fTarget, fSource));
        }
        
        // 5. Delete the Source Element
        add(new DeleteArchimateElementCommand(fSource));
    }
    
    
    /**
     * Command to reconnect a relationship to a new source or target
     */
    private static class ReconnectRelationshipCommand extends Command {
        private IArchimateRelationship fRelationship;
        private IArchimateElement fNewEndpoint;
        private IArchimateElement fOldEndpoint;
        private boolean fIsSource; // true if we are changing the source, false for target
        
        public ReconnectRelationshipCommand(IArchimateRelationship rel, IArchimateElement newEndpoint, boolean isSource) {
            fRelationship = rel;
            fNewEndpoint = newEndpoint;
            fIsSource = isSource;
            if(isSource) {
                fOldEndpoint = (IArchimateElement)rel.getSource();
            } else {
                fOldEndpoint = (IArchimateElement)rel.getTarget();
            }
            setLabel("Reconnect Relationship");
        }
        
        @Override
        public void execute() {
            if(fIsSource) {
                fRelationship.setSource(fNewEndpoint);
            } else {
                fRelationship.setTarget(fNewEndpoint);
            }
        }
        
        @Override
        public void undo() {
            if(fIsSource) {
                fRelationship.setSource(fOldEndpoint);
            } else {
                fRelationship.setTarget(fOldEndpoint);
            }
        }
    }
    
    /**
     * Command to set the underlying Archimate Element of a Diagram Object
     */
    private static class SetArchimateElementCommand extends Command {
        private IDiagramModelArchimateObject fDiagramObject;
        private IArchimateElement fNewElement;
        private IArchimateElement fOldElement;
        private CompoundCommand fSubCommands;
        
        public SetArchimateElementCommand(IDiagramModelArchimateObject dmo, IArchimateElement newElement) {
            fDiagramObject = dmo;
            fNewElement = newElement;
            fOldElement = dmo.getArchimateElement();
            setLabel("Set Diagram Object Element");
            
            // Check if successful replacement would lead to duplicate objects on the same diagram
            // If so, we need to migrate connections to the existing object and delete this one
            IDiagramModelArchimateObject existingObject = findExistingObjectOnDiagram(dmo, newElement);
            if(existingObject != null) {
                fSubCommands = new CompoundCommand();
                
                // Migrate source connections
                for(IDiagramModelConnection conn : new ArrayList<>(dmo.getSourceConnections())) {
                    fSubCommands.add(new MoveConnectionCommand(conn, existingObject, true));
                }
                
                // Migrate target connections
                for(IDiagramModelConnection conn : new ArrayList<>(dmo.getTargetConnections())) {
                    fSubCommands.add(new MoveConnectionCommand(conn, existingObject, false));
                }
                
                // Delete this object
                fSubCommands.add(new DeleteDiagramObjectCommand(dmo));
            }
        }
        
        @Override
        public void execute() {
            // If we have sub-commands (migration/deletion), execute them instead of setting the element
            if(fSubCommands != null) {
                fSubCommands.execute();
            } else {
                fDiagramObject.setArchimateElement(fNewElement);
                // Plugin-safe refresh trigger
                fDiagramObject.getFeatures().putString("merge-refresh", String.valueOf(System.currentTimeMillis()));
                fDiagramObject.getFeatures().remove("merge-refresh");
            }
        }
        
        @Override
        public void undo() {
            if(fSubCommands != null) {
                fSubCommands.undo();
            } else {
                fDiagramObject.setArchimateElement(fOldElement);
            }
        }
        
        // Find if the new element is already present on the same diagram (excluding the object being modified)
        private IDiagramModelArchimateObject findExistingObjectOnDiagram(IDiagramModelArchimateObject originalDmo, IArchimateElement element) {
            if(originalDmo.getDiagramModel() == null) {
                return null;
            }
            
            for(IDiagramModelObject dmo : originalDmo.getDiagramModel().getChildren()) {
                if(dmo != originalDmo && dmo instanceof IDiagramModelArchimateObject) {
                    if(((IDiagramModelArchimateObject)dmo).getArchimateElement() == element) {
                        return (IDiagramModelArchimateObject)dmo;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Command to move a diagram connection to a new diagram object
     */
    private static class MoveConnectionCommand extends Command {
        private IDiagramModelConnection fConnection;
        private IConnectable fNewEndpoint;
        private IConnectable fOldEndpoint;
        private boolean fIsSource; // true if moving source, false for target
        
        public MoveConnectionCommand(IDiagramModelConnection conn, IConnectable newEndpoint, boolean isSource) {
            fConnection = conn;
            fNewEndpoint = newEndpoint;
            fIsSource = isSource;
            fOldEndpoint = isSource ? conn.getSource() : conn.getTarget();
            setLabel("Move Connection");
        }
        
        @Override
        public void execute() {
            if(fIsSource) {
                fConnection.connect(fNewEndpoint, fConnection.getTarget());
            } else {
                fConnection.connect(fConnection.getSource(), fNewEndpoint);
            }
        }
        
        @Override
        public void undo() {
            if(fIsSource) {
                fConnection.connect(fOldEndpoint, fConnection.getTarget());
            } else {
                fConnection.connect(fConnection.getSource(), fOldEndpoint);
            }
        }
    }
    
    /**
     * Internal Command to delete a Diagram Object
     */
    private static class DeleteDiagramObjectCommand extends Command {
        private com.archimatetool.model.IDiagramModelContainer fParent;
        private com.archimatetool.model.IDiagramModelObject fObject;
        private int fIndex;
        
        public DeleteDiagramObjectCommand(com.archimatetool.model.IDiagramModelObject object) {
            fParent = (com.archimatetool.model.IDiagramModelContainer)object.eContainer();
            fObject = object;
        }

        @Override
        public void execute() {
            if(fParent != null && fParent.getChildren().contains(fObject)) {
                fIndex = fParent.getChildren().indexOf(fObject);
                fParent.getChildren().remove(fObject);
            }
        }
        
        @Override
        public void undo() {
            if(fParent != null && fIndex != -1) {
                fParent.getChildren().add(fIndex, fObject);
            }
        }
    }

    /**
     * Command to merge documentation and properties from source to target
     */
    private static class MergePropertiesCommand extends Command {
        private IArchimateElement fTarget;
        private IArchimateElement fSource;
        private String fOldDocumentation;
        private List<IProperty> fAddedProperties;
        
        public MergePropertiesCommand(IArchimateElement target, IArchimateElement source) {
            fTarget = target;
            fSource = source;
            setLabel("Merge Properties");
        }
        
        @Override
        public void execute() {
            // 1. Documentation
            fOldDocumentation = fTarget.getDocumentation();
            if(StringUtils.isSet(fSource.getDocumentation())) {
                String newDoc = fTarget.getDocumentation();
                if(StringUtils.isSet(newDoc)) {
                    newDoc += "\n\n" + fSource.getDocumentation();
                } else {
                    newDoc = fSource.getDocumentation();
                }
                fTarget.setDocumentation(newDoc);
            }
            
            // 2. User Properties
            fAddedProperties = new ArrayList<>();
            for(IProperty sourceProp : fSource.getProperties()) {
                if(!hasProperty(fTarget, sourceProp.getKey(), sourceProp.getValue())) {
                    IProperty newProp = IArchimateFactory.eINSTANCE.createProperty(sourceProp.getKey(), sourceProp.getValue());
                    fTarget.getProperties().add(newProp);
                    fAddedProperties.add(newProp);
                }
            }
        }
        
        @Override
        public void undo() {
            // Restore documentation
            fTarget.setDocumentation(fOldDocumentation);
            
            // Remove added properties
            fTarget.getProperties().removeAll(fAddedProperties);
        }
        
        private boolean hasProperty(IArchimateElement element, String key, String value) {
            for(IProperty p : element.getProperties()) {
                if(key.equals(p.getKey()) && value.equals(p.getValue())) {
                    return true;
                }
            }
            return false;
        }
    }

}
