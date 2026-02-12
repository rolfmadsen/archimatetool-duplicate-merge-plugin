package com.archimatetool.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.archimatetool.editor.ui.ArchiLabelProvider;
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IProperty;

/**
 * Merge Dialog
 * 
 * @author Phillip Beauvoir
 * @author AI Agent (Vibe Coded)
 */
public class MergeDialog extends ExtendedTitleAreaDialog {

    private List<IArchimateElement> fElements;
    private IArchimateElement fSelectedTarget;
    private boolean fMergeProperties = true;
    
    private TableViewer fTableViewer;
    private Button fMergePropertiesCheckbox;

    public MergeDialog(Shell parentShell, List<IArchimateElement> elements) {
        super(parentShell, "MergeDialog"); //$NON-NLS-1$
        fElements = elements;
        fSelectedTarget = elements.get(0);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText("Merge Elements");
        setTitle("Merge Elements");
        setMessage("Select the target (Main) element and merge options.\n" +
                   "Connections and ArchiMate elements from other elements will be moved to the target.");

        Composite composite = (Composite) super.createDialogArea(parent);
        Composite client = new Composite(composite, SWT.NULL);
        client.setLayout(new GridLayout(1, false));
        client.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label label = new Label(client, SWT.NULL);
        label.setText("Select the element to keep as the Merge Target:");

        Composite tableComp = new Composite(client, SWT.NULL);
        tableComp.setLayout(new TableColumnLayout());
        tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        createTable(tableComp);

        fMergePropertiesCheckbox = new Button(client, SWT.CHECK);
        fMergePropertiesCheckbox.setText("Merge Documentation and User Properties (append to target)");
        fMergePropertiesCheckbox.setSelection(fMergeProperties);
        fMergePropertiesCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return composite;
    }

    private void createTable(Composite parent) {
        fTableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        fTableViewer.getTable().setHeaderVisible(true);
        fTableViewer.getTable().setLinesVisible(true);
        
        // Enable tooltips
        ColumnViewerToolTipSupport.enableFor(fTableViewer);
        
        TableColumnLayout layout = (TableColumnLayout)parent.getLayout();

        // 1. Element
        TableViewerColumn col1 = new TableViewerColumn(fTableViewer, SWT.NONE);
        col1.getColumn().setText("Element");
        layout.setColumnData(col1.getColumn(), new ColumnWeightData(25, true));
        col1.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ArchiLabelProvider.INSTANCE.getLabel(element);
            }
            @Override
            public Image getImage(Object element) {
                return ArchiLabelProvider.INSTANCE.getImage(element);
            }
        });

        // 2. Used In (Views)
        TableViewerColumn col2 = new TableViewerColumn(fTableViewer, SWT.NONE);
        col2.getColumn().setText("Used In (Views)");
        layout.setColumnData(col2.getColumn(), new ColumnWeightData(35, true));
        col2.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return getUsedIn((IArchimateElement)element);
            }
            @Override
            public String getToolTipText(Object element) {
                return getText(element);
            }
        });

        // 3. Relations
        TableViewerColumn col3 = new TableViewerColumn(fTableViewer, SWT.NONE);
        col3.getColumn().setText("Relations");
        layout.setColumnData(col3.getColumn(), new ColumnWeightData(20, true));
        col3.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return getRelationsContext((IArchimateElement)element);
            }
            @Override
            public String getToolTipText(Object element) {
                return getText(element);
            }
        });

        // 4. Properties Preview
        TableViewerColumn col4 = new TableViewerColumn(fTableViewer, SWT.NONE);
        col4.getColumn().setText("Properties Preview");
        layout.setColumnData(col4.getColumn(), new ColumnWeightData(20, true));
        col4.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return getPropertiesPreview((IArchimateElement)element);
            }
            @Override
            public String getToolTipText(Object element) {
                IArchimateElement el = (IArchimateElement)element;
                return el.getProperties().stream()
                        .map(p -> p.getKey() + " = " + p.getValue())
                        .collect(Collectors.joining("\n"));
            }
        });

        fTableViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return fElements.toArray();
            }
            @Override public void dispose() {}
            @Override public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
        });

        fTableViewer.setInput(fElements);
        fTableViewer.setSelection(new StructuredSelection(fSelectedTarget));
        
        fTableViewer.addSelectionChangedListener(event -> {
            fSelectedTarget = (IArchimateElement) ((StructuredSelection)event.getSelection()).getFirstElement();
        });
    }

    private String getUsedIn(IArchimateElement el) {
        List<String> diagramNames = new ArrayList<>();
        for(IDiagramModelArchimateObject dmo : el.getReferencingDiagramObjects()) {
            if(dmo.getDiagramModel() != null) {
                diagramNames.add(dmo.getDiagramModel().getName());
            }
        }
        return diagramNames.stream().distinct().collect(Collectors.joining(", "));
    }

    private String getRelationsContext(IArchimateElement el) {
        List<String> rels = new ArrayList<>();
        for(IArchimateRelationship rel : el.getSourceRelationships()) {
            String relType = ArchiLabelProvider.INSTANCE.getDefaultName(rel.eClass());
            String targetName = ArchiLabelProvider.INSTANCE.getLabel(rel.getTarget());
            rels.add(relType + " -> " + targetName);
        }
        for(IArchimateRelationship rel : el.getTargetRelationships()) {
            String relType = ArchiLabelProvider.INSTANCE.getDefaultName(rel.eClass());
            String sourceName = ArchiLabelProvider.INSTANCE.getLabel(rel.getSource());
            rels.add(sourceName + " -> " + relType);
        }
        return String.join(", ", rels);
    }

    private String getPropertiesPreview(IArchimateElement el) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < el.getProperties().size() && i < 2; i++) {
            IProperty p = el.getProperties().get(i);
            if(sb.length() > 0) sb.append(", ");
            sb.append(p.getKey()).append("=").append(p.getValue());
        }
        if(el.getProperties().size() > 2) sb.append("...");
        return sb.toString();
    }

    @Override
    protected void okPressed() {
        fMergeProperties = fMergePropertiesCheckbox.getSelection();
        super.okPressed();
    }

    public IArchimateElement getSelectedTarget() {
        return fSelectedTarget;
    }

    public boolean isMergeProperties() {
        return fMergeProperties;
    }

    @Override
    protected Point getDefaultDialogSize() {
        return new Point(800, 500);
    }
}
