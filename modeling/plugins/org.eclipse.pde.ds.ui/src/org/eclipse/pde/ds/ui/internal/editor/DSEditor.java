/**
 * Copyright (c) 2009 Anyware Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Anyware Technologies - initial API and implementation
 *
 * $Id: DSEditor.java,v 1.7 2009/06/02 09:09:08 bcabe Exp $
 */
package org.eclipse.pde.ds.ui.internal.editor;

import java.io.IOException;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.pde.ds.scr.Component;
import org.eclipse.pde.ds.scr.provider.ScrItemProviderAdapterFactory;
import org.eclipse.pde.ds.ui.internal.Activator;
import org.eclipse.pde.emfforms.editor.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * The FormEditor defining the pages allowing to edit a {@link Component}
 */
public class DSEditor extends EmfFormEditor<Component> implements IResourceChangeListener {

	public static String ID = "ds.DSEditor";

	private class ResourceDeltaVisitor implements IResourceDeltaVisitor {
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta.getResource().getType() == IResource.FILE) {
				if (delta.getKind() == IResourceDelta.REMOVED) {
					String fullPath = delta.getFullPath().toString();
					final URI changedURI = URI.createPlatformResourceURI(fullPath, false);

					Resource currentResource = getCurrentEObject().eResource();
					if (currentResource.getURI().equals(changedURI)) {
						Shell currentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

						currentShell.getDisplay().asyncExec(new Runnable() {
							public void run() {
								getSite().getPage().closeEditor(DSEditor.this, false);
							}
						});
					}
				} else if (delta.getKind() == IResourceDelta.CHANGED) {
					String fullPath = delta.getFullPath().toString();
					final URI changedURI = URI.createPlatformResourceURI(fullPath, false);

					SWTObservables.getRealm(Display.getDefault()).asyncExec(new Runnable() {
						public void run() {
							EObject currentEObject = (EObject) getInputObservable().getValue();
							Resource currentResource = currentEObject.eResource();
							boolean isMainResource = currentResource.getURI().equals(changedURI);
							Resource changedResource = currentResource.getResourceSet().getResource(changedURI, false);

							// The changed resource is contained in the
							// resourceset, it must be reloaded
							if (changedResource != null && changedResource.isLoaded() && !isSaving) {

								// The editor has pending changes, we
								// must
								// inform the user, the content is going
								// to be
								// reloaded
								if (isMainResource && isDirty()) {
									getEditingDomain().getCommandStack().flush();
								}

								try {
									changedResource.unload();
									changedResource.load(Collections.EMPTY_MAP);

									// If the modified resource is the
									// main resource, we update the
									// current object
									if (isMainResource) {
										setMainResource(changedResource);
										getDataBindingContext().updateModels();
									}
								} catch (IOException ioe) {
									Activator.log(ioe);
								}
							}
						}
					});
				}

			}

			return true;
		}
	}

	private boolean isSaving = false;

	@Override
	protected DefaultEmfFormEditorConfig getFormEditorConfig() {
		return new DSEditorConfig();
	}

	@Override
	public String getPartName() {
		return getEditorInput().getName() + " (experimental)"; //$NON-NLS-1$
	}

	@Override
	public void createModel() {
		super.createModel();

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		try {
			ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
			delta.accept(visitor);
		} catch (CoreException ce) {
			Activator.log(ce);
		}

	}

	@Override
	protected List<AbstractEmfFormPage> getPagesToAdd() throws PartInitException {
		List<AbstractEmfFormPage> pages = new ArrayList<AbstractEmfFormPage>(1);

		pages.add(new OverviewPage(this));
		pages.add(new PropertiesPage(this));
		try {
			pages.add(new SourcePage(this));
		} catch (NoClassDefFoundError ex) {
			Activator.logErrorMessage("WTP SSE not available... No source page at the moment"); //$NON-NLS-1$
		}

		return pages;
	}

	@Override
	protected List<Image> getPagesImages() {
		ArrayList<Image> list = new ArrayList<Image>(1);
		list.add(null);
		return list;
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		isSaving = true;
		super.doSave(monitor);
		isSaving = false;
	}

	@Override
	protected AdapterFactory getSpecificAdapterFactory() {
		return new ScrItemProviderAdapterFactory();
	}

	@Override
	public String getID() {
		return ID;
	}
}
