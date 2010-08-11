// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.model.migration;

import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.core.model.utils.emf.talendfile.impl.ElementParameterTypeImpl;
import org.talend.repository.model.ProxyRepositoryFactory;

/**
 * DOC zli class global comment. Detailled comment <br/>
 * 
 */
public class AddConnectionVersionForJobsettingMigrationTask extends AbstractJobMigrationTask {

    public AddConnectionVersionForJobsettingMigrationTask() {
        // TODO Auto-generated constructor stub
    }

    private static final ProxyRepositoryFactory FACTORY = ProxyRepositoryFactory.getInstance();

    @Override
    public ExecutionResult execute(Item item) {
        ProcessType processType = getProcessType(item);
        EList elementParameter = processType.getParameters().getElementParameter();
        boolean implicitVersionNeed = true;
        boolean statsLogVersionNeed = true;
        String dbTypeImplicit = "";
        String dbTypeStatsLog = "";
        for (int i = 0; i < elementParameter.size(); i++) {
            final Object object = elementParameter.get(i);
            if (object instanceof ElementParameterTypeImpl) {
                ElementParameterTypeImpl parameterType = (ElementParameterTypeImpl) object;
                String name = parameterType.getName();
                if ("PROPERTY_TYPE_IMPLICIT_CONTEXT:REPOSITORY_PROPERTY_TYPE".equals(name)) { //$NON-NLS-N$
                    implicitVersionNeed = false;
                }
                if ("PROPERTY_TYPE:REPOSITORY_PROPERTY_TYPE".equals(name)) { //$NON-NLS-N$
                    statsLogVersionNeed = false;
                }
                if ("DB_TYPE_IMPLICIT_CONTEXT".equals(name)) { //$NON-NLS-N$
                    dbTypeImplicit = parameterType.getValue();
                }
                if ("DB_TYPE".equals(name)) {//$NON-NLS-N$
                    dbTypeStatsLog = parameterType.getValue();
                }
            }
        }
        if (implicitVersionNeed) {
            if (dbTypeImplicit.toUpperCase().contains("MYSQL")) { //$NON-NLS-N$
                setParameterValue(elementParameter, "DB_VERSION_IMPLICIT_CONTEXT", "mysql-connector-java-5.1.0-bin.jar"); //$NON-NLS-N$//$NON-NLS-N$
            } else if (dbTypeImplicit.toUpperCase().contains("ORACLE")) { //$NON-NLS-N$
                setParameterValue(elementParameter, "DB_VERSION_IMPLICIT_CONTEXT", "ojdbc14-10g.jar"); //$NON-NLS-N$//$NON-NLS-N$
            } else if (dbTypeImplicit.toUpperCase().contains("ACCESS")) { //$NON-NLS-N$
                setParameterValue(elementParameter, "DB_VERSION_IMPLICIT_CONTEXT", "ACCESS_2003"); //$NON-NLS-N$//$NON-NLS-N$
            }
        }
        if (statsLogVersionNeed) {
            if (dbTypeStatsLog.toUpperCase().contains("MYSQL")) { //$NON-NLS-N$
                setParameterValue(elementParameter, "DB_VERSION", "mysql-connector-java-5.1.0-bin.jar"); //$NON-NLS-N$//$NON-NLS-N$
            } else if (dbTypeStatsLog.toUpperCase().contains("ORACLE")) { //$NON-NLS-N$
                setParameterValue(elementParameter, "DB_VERSION", "ojdbc14-10g.jar"); //$NON-NLS-N$//$NON-NLS-N$
            } else if (dbTypeStatsLog.toUpperCase().contains("ACCESS")) { //$NON-NLS-N$
                setParameterValue(elementParameter, "DB_VERSION", "ACCESS_2007"); //$NON-NLS-N$//$NON-NLS-N$
            }
        }

        if (implicitVersionNeed || statsLogVersionNeed) {
            try {
                FACTORY.save(item, true);
                return ExecutionResult.SUCCESS_NO_ALERT;
            } catch (PersistenceException e) {
                ExceptionHandler.process(e);
                return ExecutionResult.FAILURE;
            }
        }
        return ExecutionResult.NOTHING_TO_DO;

    }

    private void setParameterValue(EList elementParameter, String paramName, String paramValue) {
        for (int i = 0; i < elementParameter.size(); i++) {
            final Object object = elementParameter.get(i);
            if (object instanceof ElementParameterTypeImpl) {
                ElementParameterTypeImpl parameterType = (ElementParameterTypeImpl) object;
                String name = parameterType.getName();
                if (paramName.equals(name)) {
                    parameterType.setValue(paramValue);
                }
            }
        }
    }

    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2010, 8, 10, 12, 0, 0);
        return gc.getTime();
    }
}
