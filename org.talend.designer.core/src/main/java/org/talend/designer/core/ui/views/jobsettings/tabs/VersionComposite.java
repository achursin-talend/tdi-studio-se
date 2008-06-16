// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.views.jobsettings.tabs;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.swt.actions.ITreeContextualAction;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.i18n.Messages;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.repository.ui.actions.ActionsHelper;
import org.talend.repository.ui.views.IRepositoryView;

/**
 * yzhang class global comment. Detailled comment
 */
public class VersionComposite extends AbstractTabComposite {

    private TableViewer tableViewer;

    /**
     * yzhang VersionComposite class global comment. Detailled comment
     */
    private static class IRepositoryObjectComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            return VersionUtils.compareTo(((IRepositoryObject) o1).getVersion(), ((IRepositoryObject) o2).getVersion());
        }
    }

    /**
     * yzhang VersionComposite constructor comment.
     * 
     * @param parent
     * @param style
     */
    public VersionComposite(Composite parent, int style, TabbedPropertySheetWidgetFactory factory, IRepositoryObject obj) {
        super(parent, style, factory, obj);
        FormLayout layout = new FormLayout();
        setLayout(layout);

        FormData thisFormData = new FormData();
        thisFormData.left = new FormAttachment(0, 0);
        thisFormData.right = new FormAttachment(100, 0);
        thisFormData.top = new FormAttachment(0, 0);
        thisFormData.bottom = new FormAttachment(100, 0);
        setLayoutData(thisFormData);

        tableViewer = new TableViewer(this, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        final Table table = tableViewer.getTable();
        TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        final String[] columnProperties = new String[] {
                Messages.getString("VersionSection.Version"), Messages.getString("VersionSection.CreationDate"), Messages.getString("VersionSection.ModificationDate") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        tableLayout.addColumnData(new ColumnPixelData(125, true));
        column1.setText(columnProperties[0]);

        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        tableLayout.addColumnData(new ColumnPixelData(125, true));
        column2.setText(columnProperties[1]);

        final TableColumn column3 = new TableColumn(table, SWT.NONE);
        tableLayout.addColumnData(new ColumnWeightData(1, 150, true));
        column3.setText(columnProperties[2]);

        tableViewer.setColumnProperties(columnProperties);

        Object layoutData = parent.getLayoutData();
        if (layoutData instanceof GridData) {
            GridData gridData = (GridData) layoutData;
            gridData.grabExcessVerticalSpace = true;
            gridData.verticalAlignment = SWT.FILL;
        }

        FormData formData = new FormData();
        formData.left = new FormAttachment(0);
        formData.top = new FormAttachment(0);
        formData.right = new FormAttachment(100);
        formData.bottom = new FormAttachment(100);
        table.setLayoutData(formData);

        tableViewer.setContentProvider(new IStructuredContentProvider() {

            public Object[] getElements(Object inputElement) {
                IRepositoryObject repositoryObject = ((IRepositoryObject) inputElement);
                if (repositoryObject.getProperty() == null) {
                    return null;
                }

                RepositoryNode parentRepositoryNode = getParentRepositoryNode();

                try {
                    List<IRepositoryObject> allVersion = ProxyRepositoryFactory.getInstance().getAllVersion(
                            repositoryObject.getId());
                    Collections.sort(allVersion, new IRepositoryObjectComparator());
                    Object[] objects = new Object[allVersion.size()];
                    for (int i = 0; i < objects.length; i++) {
                        IRepositoryObject repositoryObjectVersion = allVersion.get(i);
                        RepositoryNode repositoryNode = createRepositoryNode(parentRepositoryNode, repositoryObjectVersion);
                        objects[i] = repositoryNode;
                    }
                    return objects;
                } catch (PersistenceException e) {
                    return null;
                }
            }

            private RepositoryNode createRepositoryNode(RepositoryNode parentRepositoryNode,
                    IRepositoryObject repositoryObjectVersion) {
                ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(repositoryObjectVersion.getProperty()
                        .getItem());

                RepositoryNode repositoryNode = new RepositoryNode(repositoryObjectVersion, parentRepositoryNode,
                        ENodeType.REPOSITORY_ELEMENT);
                repositoryNode.setProperties(EProperties.CONTENT_TYPE, itemType);
                repositoryNode.setProperties(EProperties.LABEL, repositoryObjectVersion.getLabel());
                return repositoryNode;
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
        });

        tableViewer.setLabelProvider(new ITableLabelProvider() {

            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }

            public String getColumnText(Object element, int columnIndex) {
                RepositoryNode repositoryNode = (RepositoryNode) element;
                switch (columnIndex) {
                case 0:
                    return repositoryNode.getObject().getVersion();
                case 1:
                    if (repositoryNode.getObject().getCreationDate() != null) {
                        return FORMATTER.format(repositoryNode.getObject().getCreationDate());
                    } else {
                        return null;
                    }
                case 2:
                    if (repositoryNode.getObject().getModificationDate() != null) {
                        return FORMATTER.format(repositoryNode.getObject().getModificationDate());
                    } else {
                        return null;
                    }
                default:
                    return null;
                }
            }

            public void addListener(ILabelProviderListener listener) {
            }

            public void dispose() {
            }

            public boolean isLabelProperty(Object element, String property) {
                return false;
            }

            public void removeListener(ILabelProviderListener listener) {
            }
        });

        MenuManager menuMgr = new MenuManager("#PopUp"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager mgr) {
                ISelection selection = tableViewer.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection) selection;

                    List<ITreeContextualAction> contextualsActions = ActionsHelper.getRepositoryContextualsActions();
                    for (ITreeContextualAction action : contextualsActions) {
                        if (action.isReadAction() || action.isEditAction() || action.isPropertiesAction()) {
                            action.init(null, structuredSelection);
                            if (action.isVisible()) {
                                mgr.add(action);
                            }
                        }
                    }
                }
            }
        });
        Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
        tableViewer.getControl().setMenu(menu);

        Listener sortListener = new Listener() {

            private int direction = 1;

            public void handleEvent(Event e) {
                final TableColumn column = (TableColumn) e.widget;

                if (column == table.getSortColumn()) {
                    direction = -direction;
                }
                if (direction == 1) {
                    table.setSortDirection(SWT.DOWN);
                } else {
                    table.setSortDirection(SWT.UP);
                }

                table.setSortColumn(column);
                tableViewer.setSorter(new ViewerSorter() {

                    int index = 0;

                    @Override
                    public void sort(Viewer viewer, Object[] elements) {
                        while (index < table.getColumns().length && table.getColumn(index) != column) {
                            index++;
                        }
                        super.sort(viewer, elements);
                    }

                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        ITableLabelProvider labelProvider = (ITableLabelProvider) tableViewer.getLabelProvider();
                        String columnText = labelProvider.getColumnText(e1, index) != null ? labelProvider.getColumnText(e1,
                                index) : "";
                        String columnText2 = labelProvider.getColumnText(e2, index) != null ? labelProvider.getColumnText(e2,
                                index) : "";
                        return getComparator().compare(columnText, columnText2) * direction;
                    }
                });
            }
        };
        column1.addListener(SWT.Selection, sortListener);
        column2.addListener(SWT.Selection, sortListener);
        column3.addListener(SWT.Selection, sortListener);
        table.setSortColumn(column1);
        table.setSortDirection(SWT.DOWN);

    }

    /**
     * yzhang Comment method "getParentRepositoryNode".
     * 
     * @return
     */
    private RepositoryNode getParentRepositoryNode() {
        IRepositoryView viewPart = (IRepositoryView) getActivePage().findView(IRepositoryView.VIEW_ID);
        ISelection repositoryViewSelection = viewPart.getViewer().getSelection();
        if (!(repositoryViewSelection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection structuredSelection = (IStructuredSelection) repositoryViewSelection;
        RepositoryNode selectedRepositoryNode = (RepositoryNode) structuredSelection.getFirstElement();

        if (selectedRepositoryNode == null) {
            return null;
        }
        return selectedRepositoryNode.getParent();
    }

    /**
     * yzhang Comment method "getActivePage".
     * 
     * @return
     */
    private IWorkbenchPage getActivePage() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.ui.views.jobsettings.tabs.AbstractTabComposite#refresh()
     */
    @Override
    public void refresh() {
        super.refresh();
        if (tableViewer.getContentProvider() != null) {
            if (repositoryObject != null && repositoryObject.getProperty() != null) {
                tableViewer.setInput(repositoryObject);
            } else {
                tableViewer.setInput(null);
            }
        }
    }

    /**
     * yzhang Comment method "getTableViewer".
     * 
     * @return
     */
    public ISelection getSelection() {
        refresh();
        return this.tableViewer.getSelection();
    }

}
