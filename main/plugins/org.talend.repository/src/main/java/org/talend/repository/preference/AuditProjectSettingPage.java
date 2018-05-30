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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.swt.formtools.LabelledCombo;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.runtime.projectsetting.ProjectPreferenceManager;
import org.talend.core.service.ICommandLineService;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.preference.audit.AuditManager;
import org.talend.repository.preference.audit.SupportDBUrlStore;
import org.talend.utils.security.CryptoHelper;
import org.talend.utils.sugars.TypedReturnCode;

/**
 * created by hcyi on May 9, 2018
 * Detailled comment
 *
 */
public class AuditProjectSettingPage extends ProjectSettingPage {

    private LabelledCombo dbTypeCombo;

    private LabelledText driverText;

    private LabelledText urlText;

    private LabelledText usernameText;

    private LabelledText passwordText;

    private Button checkButton;

    private Button generateButton;

    private String generatePath;

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
        //
        createDbConfigGroup(composite);
        checkButton = new Button(composite, SWT.NULL);
        checkButton.setText(Messages.getString("AuditProjectSettingPage.DBConfig.CheckButtonText")); //$NON-NLS-1$

        generateButton = new Button(composite, SWT.NONE);
        generateButton.setText(Messages.getString("AuditProjectSettingPage.generateButtonText")); //$NON-NLS-1$
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (factory.isUserReadOnlyOnCurrentProject()) {
            composite.setEnabled(false);
        }
        addListeners();
        load();
        return composite;
    }

    protected Composite createDbConfigGroup(Composite parent) {
        GridLayout layout2 = (GridLayout) parent.getLayout();
        Group group = new Group(parent, SWT.NONE);
        group.setText(Messages.getString("AuditProjectSettingPage.DBConfig.title")); //$NON-NLS-1$
        GridDataFactory.fillDefaults().span(layout2.numColumns, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false)
                .applyTo(group);
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        group.setLayout(layout);
        //
        dbTypeCombo = new LabelledCombo(group, Messages.getString("AuditProjectSettingPage.DBConfig.dbType"), //$NON-NLS-1$
                Messages.getString("AuditProjectSettingPage.DBConfig.dbTypeTip"), //$NON-NLS-1$
                SupportDBUrlStore.getInstance().getDBDisplayNames(), 2, 
                true);
        driverText = new LabelledText(group, Messages.getString("AuditProjectSettingPage.DBConfig.Driver"), 2); //$NON-NLS-1$
        driverText.setEditable(false);
        urlText = new LabelledText(group, Messages.getString("AuditProjectSettingPage.DBConfig.Url"), 2); //$NON-NLS-1$
        usernameText = new LabelledText(group, Messages.getString("AuditProjectSettingPage.DBConfig.Username"), 2); //$NON-NLS-1$
        passwordText = new LabelledText(group, Messages.getString("AuditProjectSettingPage.DBConfig.Password"), 2, //$NON-NLS-1$
                SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        return group;
    }

    private void addListeners() {
        dbTypeCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                String selectedItem = ((Combo) e.getSource()).getText();
                String dbType = SupportDBUrlStore.getInstance().getDBType(selectedItem);
                driverText.setText(SupportDBUrlStore.getInstance().getDBUrlType(dbType).getDbDriver());
                urlText.setText(SupportDBUrlStore.getInstance().getDefaultDBUrl(dbType));
            }
        });

        checkButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (GlobalServiceRegister.getDefault().isServiceRegistered(ICommandLineService.class)) {
                    ICommandLineService service = (ICommandLineService) GlobalServiceRegister.getDefault()
                            .getService(ICommandLineService.class);
                    TypedReturnCode<java.sql.Connection> result = service.checkConnection(urlText.getText(), driverText.getText(),
                            usernameText.getText(),
                            passwordText.getText());
                    MessageDialog.openInformation(getShell(),
                            Messages.getString("AuditProjectSettingPage.DBConfig.CheckButtonText"), result.getMessage()); //$NON-NLS-1$
                }
            }

        });
        generateButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dial = new DirectoryDialog(getShell(), SWT.NONE);
                String directory = dial.open();
                if (StringUtils.isNotEmpty(directory)) {
                    generatePath = Path.fromOSString(directory).toPortableString();
                } else {
                    MessageDialog.openError(getShell(), "Error", //$NON-NLS-1$
                            Messages.getString("AuditProjectSettingPage.selectAuditReportFolder")); //$NON-NLS-1$
                    return;
                }

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
                                    String path = "";//$NON-NLS-1$
                                    File tempFolder = null;
                                    try {
                                        File createTempFile = File.createTempFile("AuditReport", ""); //$NON-NLS-1$ //$NON-NLS-2$
                                        path = createTempFile.getPath();
                                        createTempFile.delete();
                                        tempFolder = new File(path);
                                        tempFolder.mkdir();
                                        path = path.replace("\\", "/");//$NON-NLS-1$//$NON-NLS-2$

                                        service.populateAudit(urlText.getText(), driverText.getText(), usernameText.getText(),
                                                passwordText.getText());
                                        service.generateAuditReport(generatePath);
                                    } catch (IOException e) {
                                        // nothing
                                    } finally {
                                        FilesUtils.deleteFile(tempFolder, true);
                                    }
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

    private void load() {
        ProjectPreferenceManager prefManager = new ProjectPreferenceManager(AuditManager.AUDIT_RESOURCES, true);
        dbTypeCombo.setText(prefManager.getValue(AuditManager.AUDIT_DBTYPE));
        driverText.setText(prefManager.getValue(AuditManager.AUDIT_DRIVER));
        urlText.setText(prefManager.getValue(AuditManager.AUDIT_URL));
        usernameText.setText(prefManager.getValue(AuditManager.AUDIT_USERNAME));
        passwordText.setText(CryptoHelper.getDefault().decrypt(prefManager.getValue(AuditManager.AUDIT_PASSWORD)));
    }

    private void save() {
        ProjectPreferenceManager prefManager = new ProjectPreferenceManager(AuditManager.AUDIT_RESOURCES, true);
        prefManager.setValue(AuditManager.AUDIT_DBTYPE, dbTypeCombo.getText());
        prefManager.setValue(AuditManager.AUDIT_DRIVER, driverText.getText());
        prefManager.setValue(AuditManager.AUDIT_URL, urlText.getText());
        prefManager.setValue(AuditManager.AUDIT_USERNAME, usernameText.getText());
        prefManager.setValue(AuditManager.AUDIT_PASSWORD, CryptoHelper.getDefault().encrypt(passwordText.getText()));
        prefManager.save();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        save();
        super.performApply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        save();
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
