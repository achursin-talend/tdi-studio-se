package org.talend.sdk.component.studio.metadata.handler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.designer.core.model.utils.emf.talendfile.impl.NodeTypeImpl;
import org.talend.designer.core.model.utils.emf.talendfile.impl.ProcessTypeImpl;
import org.talend.repository.ProjectManager;
import org.talend.repository.items.importexport.handlers.imports.IImportResourcesHandler;
import org.talend.repository.items.importexport.handlers.imports.ImportCacheHelper;
import org.talend.repository.items.importexport.handlers.model.ImportItem;
import org.talend.repository.items.importexport.manager.ResourcesManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.websocket.WebSocketClient.V1Component;
import org.talend.sdk.studio.process.TaCoKitNode;

public class TaCoKitProcessMigrator implements IImportResourcesHandler {
    
    private final V1Component client = Lookups.client().v1().component(); 

    @Override
    public void prePopulate(IProgressMonitor monitor, ResourcesManager resManager) {
        // no-op
    }

    @Override
    public void postPopulate(IProgressMonitor monitor, ResourcesManager resManager, ImportItem[] populatedItemRecords) {
        // no-op
    }

    @Override
    public void preImport(IProgressMonitor monitor, ResourcesManager resManager, ImportItem[] checkedItemRecords,
            ImportItem[] allImportItemRecords) {
        // no-op
    }

    @Override
    public void postImport(IProgressMonitor monitor, ResourcesManager resManager, ImportItem[] importedItemRecords) {
        if (importedItemRecords == null) {
            return;
        }
        for (final ImportItem importItem : importedItemRecords) {
            try {
                getItem(importItem).ifPresent(processItem -> {
                    migrateItem(processItem);
                });
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
        }
    }
    
    /**
     * Loads repository item. Implementation was copied from
     * ImportBasicHandler.applyMigrationTasks()
     * 
     * @param importItem
     *            item which is imported
     * @return Process item stored in repository
     * @throws PersistenceException
     */
    private Optional<ProcessItem> getItem(final ImportItem importItem) throws PersistenceException {
        List<IRepositoryViewObject> allVersion = ProxyRepositoryFactory.getInstance().getAllVersion(
                ProjectManager.getInstance().getCurrentProject(), importItem.getItemId(), importItem.getImportPath(),
                importItem.getRepositoryType());
        for (IRepositoryViewObject repositoryObject : allVersion) {
            if (repositoryObject.getProperty().getVersion().equals(importItem.getItemVersion())) {
                final Item item = repositoryObject.getProperty().getItem();
                if (ProcessItem.class.isInstance(item)) {
                    return Optional.of((ProcessItem) item);
                }
            }
        }
        return Optional.empty();
    }
    
    private void migrateItem(final ProcessItem item) {
        final ProcessTypeImpl processType = (ProcessTypeImpl) item.getProcess();
        migrateProcess(processType);
        save(item);
    }
    
    private void migrateProcess(final ProcessTypeImpl process) {
        migrateNodes(process.getNode());
    }
    
    @SuppressWarnings("rawtypes")
    private void migrateNodes(final EList nodes) {
        for (final Object elem : nodes) {
            NodeTypeImpl node = (NodeTypeImpl) elem;
            if (TaCoKitNode.isTacokit(node)) {
                final TaCoKitNode tacokitNode = new TaCoKitNode(node);
                if (tacokitNode.needsMigration()) {
                    migrateNode(tacokitNode);
                }
            }
        }
    }
    
    private void migrateNode(final TaCoKitNode node) {
        final Map<String, String> migratedProperties = client.migrate(node.getId(), node.getPersistedVersion(),
                node.getProperties());
        node.migrate(migratedProperties);
    }
    
    private void save(final Item item) {
        final IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        try {
            factory.save(item);
        } catch (PersistenceException e) {
            logError(e);
        }
    }
    
    protected void logError(Exception e) {
        ImportCacheHelper.getInstance().setImportingError(true);
        ExceptionHandler.process(e);
    }

}
