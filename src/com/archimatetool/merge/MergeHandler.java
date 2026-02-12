package com.archimatetool.merge;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.model.IAdapter;
import com.archimatetool.model.IArchimateElement;

/**
 * Merge Handler
 */
public class MergeHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        IStructuredSelection structSelection = (IStructuredSelection) selection;

        // Need at least 2 elements to merge
        if (structSelection.size() < 2) {
            return null;
        }

        IArchimateElement target = null;
        List<IArchimateElement> sources = new ArrayList<>();

        for (Object obj : structSelection.toList()) {
            if (!(obj instanceof IArchimateElement)) {
                MessageDialog.openError(window.getShell(), "Merge Elements",
                        "All selected items must be ArchiMate elements.");
                return null;
            }

            IArchimateElement element = (IArchimateElement) obj;

            if (target == null) {
                target = element;
            } else {
                // Check classes are same
                if (target.eClass() != element.eClass()) {
                    MessageDialog.openError(window.getShell(), "Merge Elements",
                            "All selected elements must be of the same type.");
                    return null;
                }

                // Check names are same
                if (target.getName() == null || !target.getName().equals(element.getName())) {
                    MessageDialog.openError(window.getShell(), "Merge Elements",
                            "All selected elements must have the same name.");
                    return null;
                }

                sources.add(element);
            }
        }

        // Launch MergeDialog
        List<IArchimateElement> elements = new ArrayList<>();
        for (Object obj : structSelection.toList()) {
            elements.add((IArchimateElement) obj);
        }

        MergeDialog dialog = new MergeDialog(window.getShell(), elements);
        if (dialog.open() != org.eclipse.jface.window.Window.OK) {
            return null;
        }

        target = dialog.getSelectedTarget();
        boolean mergeProperties = dialog.isMergeProperties();

        // Create Compound Command
        CompoundCommand compoundCommand = new CompoundCommand("Merge Elements");
        for (IArchimateElement element : elements) {
            if (element != target) {
                compoundCommand.add(new MergeCommand(target, element, mergeProperties));
            }
        }

        CommandStack stack = (CommandStack) ((IAdapter) target).getAdapter(CommandStack.class);
        if (stack != null) {
            stack.execute(compoundCommand);
        }

        return null;
    }
}
