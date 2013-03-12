package ch.gleisdrei.e4.tooling.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

public class GenerateConstantsHandler extends AbstractHandler {

	private static final String JAVA_SUFFIX = ".java";
	private static final String CLASS_NAME = "ApplicationConstants";
	private Set<String> ids;
	private IPath destPackagePath;
	private String destPackageName;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);
		Shell shell = HandlerUtil.getActiveShell(event);
		IFile file = (IFile) selection.getFirstElement();

		URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(),
				true);
		ResourceSetImpl set = new ResourceSetImpl();
		Resource modelResource = set.createResource(uri);
		try {
			modelResource.load(Collections.emptyMap());
			EObject root = (EObject) modelResource.getContents().get(0);
			TreeIterator<EObject> eAllContents = root.eAllContents();

			ids = new TreeSet<String>();

			SelectionDialog dialog = JavaUI.createPackageDialog(shell,
					JavaCore.create(file.getProject()), SWT.NONE);
			dialog.open();
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				PackageFragment fragment = (PackageFragment) result[0];
				destPackageName = fragment.getElementName();
				destPackagePath = fragment.getPath();
			}
			while (eAllContents.hasNext()) {
				EObject next = eAllContents.next();
				if (next instanceof MApplicationElement) {
					MApplicationElement element = (MApplicationElement) next;
					ids.add(element.getElementId());
				}
			}
			writeClass();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		} finally {
			modelResource.unload();
		}
		return null;
	}

	private void writeClass() throws IOException {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IFolder packageFolder = ws.getRoot().getFolder(destPackagePath);
		File file = new File(packageFolder.getLocation().toFile(), CLASS_NAME
				+ JAVA_SUFFIX);
		BufferedWriter writer = null;
		try {
			writer = Files.newWriter(file, Charsets.UTF_8);

			writer.write("/* AUTO-GENERATED FILE. DO NOT MODIFY.\n");
			writer.write(" *\n");
			writer.write(" *\n");
			writer.write(" */\n");

			writer.write("package ");
			writer.write(destPackageName);
			writer.write(";\n\npublic final class " + CLASS_NAME + " {\n");

			for (String id : ids) {
				writer.write("\tpublic static final String ");
				writer.write(CaseFormat.LOWER_UNDERSCORE.to(
						CaseFormat.UPPER_UNDERSCORE, CharMatcher.is('.')
								.replaceFrom(id, "_")));
				writer.write(" = \"" + id);
				writer.write("\";\n");
			}
			writer.write("}\n");
		} finally {
			Closeables.closeQuietly(writer);
		}
	}
}