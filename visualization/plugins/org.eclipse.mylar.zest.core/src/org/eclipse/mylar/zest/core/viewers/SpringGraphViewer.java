/*******************************************************************************
 * Copyright 2005, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.mylar.zest.core.viewers;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylar.zest.core.ZestStyles;
import org.eclipse.mylar.zest.core.internal.graphmodel.GraphModel;
import org.eclipse.mylar.zest.core.internal.graphmodel.GraphModelEntityFactory;
import org.eclipse.mylar.zest.core.internal.graphmodel.GraphModelEntityRelationshipFactory;
import org.eclipse.mylar.zest.core.internal.graphmodel.GraphModelFactory;
import org.eclipse.mylar.zest.core.internal.graphmodel.IGraphModelNode;
import org.eclipse.mylar.zest.core.internal.graphmodel.IStylingGraphModelFactory;
import org.eclipse.mylar.zest.core.internal.graphviewer.SpringGraphViewerImpl;
import org.eclipse.mylar.zest.layouts.LayoutAlgorithm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 * A very springy viewer.
 * 
 * @author Ian Bull
 * @author Chris Callendar
 */
public class SpringGraphViewer extends AbstractStructuredGraphViewer {
	
	
	private SpringGraphViewerImpl viewer = null;
	private IStylingGraphModelFactory modelFactory = null;

	/**
	 * Initializes the viewer.  
	 * @param composite	
	 * @param style	The styles for this viewer (also passed on to the viewer impl) 
	 * @see ZestStyles#NODES_HIGHLIGHT_ADJACENT
	 * @see ZestStyles#PANNING
	 * @see ZestStyles#MARQUEE_SELECTION
	 * @see ZestStyles#NO_OVERLAPPING_NODES
	 * @see ZestStyles#STABILIZE
	 * @see ZestStyles#ENFORCE_BOUNDS
	 * @see ZestStyles#DIRECTED_GRAPH
	 */
	public SpringGraphViewer(Composite composite, int style) {
		super(style);
		this.viewer = new SpringGraphViewerImpl(composite, style);
		hookControl( this.viewer.getControl() );
	}
	
	/**
	 * Gets the styles for this structuredViewer
	 * @return
	 */
	public int getStyle() {
		return this.viewer.getStyle();
	}
	
	/**
	 * Sets the style on this structuredViewer
	 * @param style
	 * @return
	 */
	public void setStyle( int style ) {
		this.viewer.setStyle( style );
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ContentViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
	 */
	public void setContentProvider(IContentProvider contentProvider) {
		if (contentProvider instanceof IGraphContentProvider) {
			super.setContentProvider(contentProvider);
		} else if ( contentProvider instanceof IGraphEntityContentProvider ) {
			super.setContentProvider( contentProvider );
		} else if (contentProvider instanceof IGraphEntityRelationshipContentProvider){
			super.setContentProvider(contentProvider);
		} else {
			throw new IllegalArgumentException(
					"Invalid content provider, only IGraphContentProvider, IGraphEntityContentProvider, or IGraphEntityRelationshipContentProvider are supported.");
		}
	}

	protected void inputChanged(Object input, Object oldInput) {
		viewer.stopLayoutAlgorithm();
		//boolean highlightAdjacentNodes = ZestStyles.checkStyle(viewer.getStyle(), ZestStyles.NODES_HIGHLIGHT_ADJACENT);
		if ( getContentProvider() instanceof IGraphContentProvider ) {
			modelFactory = new GraphModelFactory( this );
		}
		else if ( getContentProvider() instanceof IGraphEntityContentProvider ) {
			modelFactory = new GraphModelEntityFactory( this );
		}
		else if (getContentProvider() instanceof IGraphEntityRelationshipContentProvider) {
//		@tag zest.bug.154580-Content.fix : add new factory here.
			modelFactory = new GraphModelEntityRelationshipFactory(this);
		}
		GraphModel model = createModel();
		modelFactory.setConnectionStyle(getConnectionStyle());
		modelFactory.setNodeStyle(getNodeStyle());
		model = modelFactory.createGraphModel();

		// set the model contents (initializes the layout algorithm)
		model.setNodeStyle(getNodeStyle());
		model.setConnectionStyle(getConnectionStyle());
		viewer.setContents(model, modelFactory);
		
	}
	
	/**
	 * Sets a single node as selected and moves it to the center.
	 * While selected the node will be pinned! 
	 * @param itemToCenter
	 */
	public void setCenterSelection( Object itemToCenter, int x, int y ) {
		if( getModel().getInternalNode( itemToCenter ) == null ) {
			// noop
		} else {
			viewer.setCenterSelection( getModel().getInternalNode(itemToCenter), x, y );
		}
	}

	
	/**
	 * Gets the (first) selected node object.  If no nodes are selected
	 * then null is returned.
	 * @return Object the external node data object.
	 */
	public Object getSelectedNode() {
		Object node = null;
		if (getSelection() instanceof StructuredSelection) {
			StructuredSelection selection = (StructuredSelection) getSelection();
			node = selection.getFirstElement();
		}
		return node;
	}
	
	// @tag TODO : Reduce the visibility of getViewer
	public SpringGraphViewerImpl getViewer() {
		return viewer;
	}
	
	/**
	 * Adds a new node to the graph
	 * @param o The new node to add
	 */
	public void addNode( Object o ) {
		viewer.addNode( o );
	}
	
	/**
	 * Adds a set of new nodes to the graph
	 * @param o An arry of nodes to add
	 */
	public void addNode( Object[] o ) {
		for ( int i = 0; i < o.length; i++ ) {
			addNode( o[i] );
		}
	}
	
	/**
	 * Adds a new relalationship to the graph
	 * @param connection The connection (or null)
	 * @param src The src node
	 * @param dest The dest node
	 * 
	 * If Connection is not null then src and dest can be quiried from the content
	 * provider
	 */
	public void addRelationship( Object connection, Object src, Object dest ) {
		viewer.addRelationship( connection, src, dest );
	}
	
	public void zoomIn() {
		viewer.zoomIn();
	}
	
	public void zoomOut() {
		viewer.zoomOut();
	}
	
	/**
	 * Updates the relationship's weight.
	 * @param relationship the data object for the relationship to update
	 * @param weight the new weight
	 */
	public void updateRelationshipWeight(Object relationship, double weight) {
		viewer.updateRelationshipWeight(relationship, weight);
	}	

	// StructuredViewer overrides
	
	public Control getControl() {
		return viewer.getControl();
	}
	
//	/**
//	 * Gets the selected model elements as a List.
//	 * @return List
//	 */
//	protected List getSelectionFromWidget() {
//		Widget[] items = viewer.getSelectedModelElements();
//		ArrayList list = new ArrayList(items.length);
//		for (int i = 0; i < items.length; i++) {
//			Widget item = items[i];
//			Object e = item.getData();
//			if (e != null)
//				list.add(e);
//		}
//		return list;
//	}
//
//	protected void setSelectionToWidget(List l, boolean reveal) {
//		ArrayList widgetList = new ArrayList( l.size() );
//		for ( int i = 0; i < l.size(); i++ ) {
//			widgetList.add(i, getModel().getInternalNode( l.get( i )));
//		}
//		//DebugPrint.println("Highlighting: " + widgetList.size() + " starting wtih: " + l.size() );
//		viewer.setSelection(new StructuredSelection( widgetList ));
//	}
//	
//	/**
//	 * Returns the input item (in this case the GraphModel object) if the given element
//	 * equals the root element, otherwise null is returned.
//	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindInputItem(java.lang.Object)
//	 */
//	protected Widget doFindInputItem(Object element) {
//		if (equals(element, getRoot())) {
//			return getModel();
//		}
//		return null;
//	}
//
//	/**
//	 * Returns the node (widget) for the given element.
//	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindItem(java.lang.Object)
//	 */
//	protected Widget doFindItem(Object element) {
//		if (getModel() != null) {
//			Object o = getModel().getInternalNode( element );
//			if (o instanceof Widget)
//				return (Widget)o;
//		}
//		return null;
//	}
//
//	/* (non-Javadoc)
//	 * @see org.eclipse.jface.viewers.StructuredViewer#doUpdateItem(org.eclipse.swt.widgets.Widget, java.lang.Object, boolean)
//	 */
//	protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
//		if (widget instanceof GraphItem) {
//			final GraphItem item = (GraphItem)widget;
//			if (fullMap) {
//				associate(element, item);
//			} else {
//				item.setData(element);
//				mapElement(element, item);
//			}	
//		}
//	}
//
//	/**
//	 * Refreshes the given element.  If this element is the root element
//	 * then all the nodes are refreshed, otherwise just the one element is refreshed.
//	 * @param element the element to refresh. 
//	 */
//	protected void internalRefresh(Object element) {
//		//DebugPrint.println("internalRefresh");
//		if ((element == null) || equals(element, getRoot())) {
//			internalRefreshAll();
//		} else {
//			Widget widget = findItem(element);
//			if (widget != null) {
//				updateItem(widget, element);
//			}
//		}
//	}
//	
//	/**
//	 * Refreshes all the nodes in the model.
//	 */
//	protected void internalRefreshAll() {
//		if (model != null) {
//			List nodes = model.getNodes();
//			for (Iterator iter = nodes.iterator(); iter.hasNext(); ) {
//				IGraphModelNode node = (IGraphModelNode)iter.next();
//				Object o = node.getExternalNode();
//				if (node instanceof Widget) {
//					updateItem((Widget)node, o);
//				}
//			}
//		}
//	}
//
//	/**
//	 * Centers the given element on the canvas.
//	 */
	public void reveal(Object element) {
		if ((getModel() != null) && (element != null)) {
			IGraphModelNode nodeToCenter = getModel().getInternalNode(element);
			if (nodeToCenter != null) {
				viewer.centerNodeInCanvas(nodeToCenter);
			}
		}
	}

	/** 
	 * SpringGraphViewer is designed for spring graph layouts only. This method is ignored.
	 */
	public void setLayoutAlgorithm(LayoutAlgorithm algorithm, boolean run) {
				
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.core.viewers.AbstractZoomableViewer#getZoomManager()
	 */
	protected ZoomManager getZoomManager() {
		//@tag bug.156286-Zooming.todo : add zooming capabilities on the SpringGraphViewer
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.core.viewers.AbstractStructuredGraphViewer#applyLayout()
	 */
	public void applyLayout() {
		//do nothing spring viewers always are layed out?
		//@tag zest.check : how does the spring graph viewer layout actually work?
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.core.viewers.AbstractStructuredGraphViewer#getLayoutAlgorithm()
	 */
	protected LayoutAlgorithm getLayoutAlgorithm() {
		return viewer.getLayoutAlgorithm();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.core.viewers.AbstractStructuredGraphViewer#getEditPartViewer()
	 */
	protected EditPartViewer getEditPartViewer() {
		return viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.core.viewers.AbstractStructuredGraphViewer#getFactory()
	 */
	protected IStylingGraphModelFactory getFactory() {
		if (modelFactory == null) {
			if (getContentProvider() instanceof IGraphContentProvider) {
				modelFactory = new GraphModelFactory(this);
			} else if (getContentProvider() instanceof IGraphEntityContentProvider) {
				modelFactory = new GraphModelEntityFactory(this);
			} else if (getContentProvider() instanceof IGraphEntityRelationshipContentProvider) {
				modelFactory = new GraphModelEntityRelationshipFactory(this);
			}
		}
		return modelFactory;
	}

}
	