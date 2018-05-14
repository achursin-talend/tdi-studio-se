// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.preference;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.service.ICommandLineService;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * created by hcyi on May 9, 2018
 * Detailled comment
 *
 */
public class AuditProjectSettingPage extends ProjectSettingPage {

    private Button browseBtn;

    private LabelledText generateFolderTxt;

    private Button button;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(4, false);
        composite.setLayout(layout);

        generateFolderTxt = new LabelledText(composite, Messages.getString("AuditProjectSettingPage.selectAuditReportFolder"), 2); //$NON-NLS-1$
        generateFolderTxt.getTextControl().setEditable(false);

        browseBtn = new Button(composite, SWT.NONE);
        browseBtn.setText("..."); //$NON-NLS-1$

        button = new Button(composite, SWT.NONE);
        button.setText(Messages.getString("AuditProjectSettingPage.generateAuditReport")); //$NON-NLS-1$
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (factory.isUserReadOnlyOnCurrentProject()) {
            composite.setEnabled(false);
        }
        addListeners();
        return composite;
    }

    private void addListeners() {
        browseBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dial = new DirectoryDialog(getShell(), SWT.NONE);
                String directory = dial.open();
                if (StringUtils.isNotEmpty(directory)) {
                    String portableValue = Path.fromOSString(directory).toPortableString();
                    generateFolderTxt.setText(portableValue);
                }
            }
        });

        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(
                        PlatformUI.getWorkbench().getDisplay().getActiveShell().getShell());
                IRunnableWithProgress runnable = new IRunnableWithProgress() {

                    @Override
                    public void run(IProgressMonitor monitor) {
                        monitor.beginTask(Messages.getString("AuditProjectSettingPage.generateAuditReportProgressBar"), //$NON-NLS-1$
                                IProgressMonitor.UNKNOWN);
                        Display.getDefault().syncExec(new Runnable() {

                            @Override
                            public void run() {
                                if (GlobalServiceRegister.getDefault().isServiceRegistered(ICommandLineService.class)) {
                                    ICommandLineService service = (ICommandLineService) GlobalServiceRegister.getDefault()
                                            .getService(ICommandLineService.class);
                                    final Bundle b = Platform.getBundle(RepositoryPlugin.PLUGIN_ID);
                                    String path = "";//$NON-NLS-1$
                                    if (b != null) {
                                        try {
                                            URL url = FileLocator.toFileURL(FileLocator.find(b, new Path("/"), null));//$NON-NLS-1$
                                            if (url != null) {
                                                path = url.getPath();
                                            }
                                        } catch (IOException ex) {
                                            //
                                        }
                                    }
                                    // Just use the h2 as default now, later will add support for others
                                    service.populateAudit("populateAudit -ju 'jdbc:h2:" + path //$NON-NLS-1$
                                            + "database/audit;AUTO_SERVER=TRUE;lock_timeout=15000' -dd 'org.h2.Driver' -du 'tisadmin' -up 'tisadmin'"); //$NON-NLS-1$
                                    service.generateAuditReport("generateAuditReport 'auditId' -fp 'filePath' -t 'default'", //$NON-NLS-1$
                                            generateFolderTxt.getText());
                                }
                            }
                        });
                        monitor.done();
                    }
                };
                try {
                    progressDialog.run(true, true, runnable);
                } catch (InvocationTargetException e1) {
                    ExceptionHandler.process(e1);
                } catch (InterruptedException e1) {
                    ExceptionHandler.process(e1);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        super.performApply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        return super.performOk();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.preference.ProjectSettingPage#refresh()
     */
    @Override
    public void refresh() {
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
    }
}
