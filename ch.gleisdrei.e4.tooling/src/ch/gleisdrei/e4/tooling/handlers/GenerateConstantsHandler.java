package ch.gleisdrei.e4.tooling.handlers;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class GenerateConstantsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);
		final IFile file = (IFile) selection.getFirstElement();
		
		URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
		ResourceSetImpl set = new ResourceSetImpl();
		Resource modelResource = set.createResource(uri);
		try {
			modelResource.load(Collections.emptyMap());
			EObject root = (EObject) modelResource.getContents().get(0);
			TreeIterator<EObject> eAllContents = root.eAllContents();
			while (eAllContents.hasNext()) {
				EObject next = eAllContents.next();
				if (next instanceof MApplicationElement) {
					MApplicationElement element = (MApplicationElement) next;
					System.out.println(element.getElementId());
				} else {
					System.out.println("\t" + next);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			modelResource.unload();
		}
		return null;
	}
}
