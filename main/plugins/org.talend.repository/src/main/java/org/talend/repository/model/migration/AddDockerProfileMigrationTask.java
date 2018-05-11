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
package org.talend.repository.model.migration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.PluginChecker;
import org.talend.core.model.general.Project;
import org.talend.core.model.migration.AbstractProjectMigrationTask;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.utils.ItemResourceUtil;
import org.talend.core.runtime.projectsetting.IProjectSettingPreferenceConstants;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.core.runtime.projectsetting.ProjectPreferenceManager;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.tools.AggregatorPomsHelper;
import org.talend.designer.maven.utils.PomUtil;

public class AddDockerProfileMigrationTask extends AbstractProjectMigrationTask {

    private final String DOCKER_PROFILE_ID = "docker"; //$NON-NLS-1$

    private final String RENAMED_DOCKER_PROFILE_ID = "docker-old"; //$NON-NLS-1$

    private final MavenModelManager MODEL_MANAGER;

    private final ProxyRepositoryFactory FACTORY;

    public AddDockerProfileMigrationTask() {
        super();
        MODEL_MANAGER = MavenPlugin.getMavenModelManager();
        FACTORY = ProxyRepositoryFactory.getInstance();
    }

    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2018, 5, 18, 12, 00, 00);
        return gc.getTime();
    }

    @Override
    public ExecutionResult execute(Project project) {
        try {
            IFile projectPom = new AggregatorPomsHelper(project.getTechnicalLabel()).getProjectRootPom();
            if (projectPom.exists()) {
                // can consider version as 7.0.1, should update all poms.
                updateProjectPom(project);
                updateAllJobsPom(project);
                return ExecutionResult.SUCCESS_NO_ALERT;
            } else {
                // if version <= 6.5.x, all jobs pom will be generated in GenerateJobPomMigrationTask later.
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
            return ExecutionResult.FAILURE;
        }
        return ExecutionResult.NOTHING_TO_DO;
    }

    private void updateProjectPom(Project project) throws CoreException, Exception {
        Profile templateProfile = new Profile();
        templateProfile.setId(DOCKER_PROFILE_ID);
        // update project pom template
        ProjectPreferenceManager projectPrefManager = new ProjectPreferenceManager(project, "org.talend.designer.maven.ui"); //$NON-NLS-1$
        String templateStr = projectPrefManager.getValue(IProjectSettingPreferenceConstants.TEMPLATE_PROJECT_POM);
        if (!StringUtils.isBlank(templateStr)) {
            InputStream inputStream = new ByteArrayInputStream(templateStr.getBytes(TalendMavenConstants.DEFAULT_ENCODING));
            Model currentTemplateModel = MODEL_MANAGER.readMavenModel(inputStream);
            replaceDockerProfile(currentTemplateModel, templateProfile);
            OutputStream outputStream = new ByteArrayOutputStream();
            MavenPlugin.getMaven().writeModel(currentTemplateModel, outputStream);
            projectPrefManager.setValue(IProjectSettingPreferenceConstants.TEMPLATE_PROJECT_POM, outputStream.toString());
        }
        // update project pom
        IFile pomFile = new AggregatorPomsHelper(project.getTechnicalLabel()).getProjectRootPom();
        Model model = MODEL_MANAGER.readMavenModel(pomFile);
        replaceDockerProfile(model, templateProfile);
        PomUtil.savePom(null, model, pomFile);
    }

    private void updateAllJobsPom(Project project) throws Exception {
        InputStream stream = null;
        Profile templateProfile = null;
        try {
            stream = MavenTemplateManager.getBundleTemplateStream(PluginChecker.MAVEN_JOB_PLUGIN_ID,
                    IProjectSettingTemplateConstants.PATH_STANDALONE + '/' // $NON-NLS-1$
                            + IProjectSettingTemplateConstants.POM_JOB_TEMPLATE_FILE_NAME);
            Model jobTemplateModel = MODEL_MANAGER.readMavenModel(stream);
            for (Profile profile : jobTemplateModel.getProfiles()) {
                if (profile.getId().equals(DOCKER_PROFILE_ID)) {
                    templateProfile = profile;
                    break;
                }
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // update default job pom template
        ProjectPreferenceManager projectPrefManager = new ProjectPreferenceManager(project, "org.talend.designer.maven.job"); //$NON-NLS-1$
        String templateStr = projectPrefManager.getValue(IProjectSettingPreferenceConstants.TEMPLATE_STANDALONE_JOB_POM);
        if (!StringUtils.isBlank(templateStr)) {
            InputStream inputStream = new ByteArrayInputStream(templateStr.getBytes(TalendMavenConstants.DEFAULT_ENCODING));
            Model currentTemplateModel = MODEL_MANAGER.readMavenModel(inputStream);

            replaceDockerProfile(currentTemplateModel, templateProfile);
            updateJobPomProperties(currentTemplateModel, "talend.job.folder", "@JobFolder@"); //$NON-NLS-1$ //$NON-NLS-2$

            OutputStream outputStream = new ByteArrayOutputStream();
            MavenPlugin.getMaven().writeModel(currentTemplateModel, outputStream);
            projectPrefManager.setValue(IProjectSettingPreferenceConstants.TEMPLATE_STANDALONE_JOB_POM, outputStream.toString());
        }

        // update custom job pom template
        IProject fsProject = ResourceUtils.getProject(project);
        updateJobCustomTemplateFile(fsProject, ERepositoryObjectType.PROCESS, templateProfile);
        updateJobCustomTemplateFile(fsProject, ERepositoryObjectType.PROCESS_MR, templateProfile);
        updateJobCustomTemplateFile(fsProject, ERepositoryObjectType.PROCESS_STORM, templateProfile);

        // update all job pom
        List<IRepositoryViewObject> objects = new ArrayList<>();
        for (ERepositoryObjectType type : getTypesOfProcess()) {
            objects.addAll(FACTORY.getAll(type));
        }
        for (IRepositoryViewObject object : objects) {
            Property property = object.getProperty();
            IFile pomFile = AggregatorPomsHelper.getItemPomFolder(property).getFile(TalendMavenConstants.POM_FILE_NAME);
            if (!pomFile.exists()) {
                // should not happened, even if it did, do nothing.
                continue;
            }
            Model model = MODEL_MANAGER.readMavenModel(pomFile);

            replaceDockerProfile(model, templateProfile);
            String jobFolderPath = ItemResourceUtil.getItemRelativePath(property).toPortableString();
            if (!StringUtils.isEmpty(jobFolderPath)) {
                // like f1/f2/f3/
                jobFolderPath = StringUtils.strip(jobFolderPath, "/") + "/"; // $NON-NLS-1$ // $NON-NLS-2$
            }
            updateJobPomProperties(model, "talend.job.folder", jobFolderPath); // $NON-NLS-1$

            PomUtil.savePom(null, model, pomFile);
        }
    }

    private void replaceDockerProfile(Model model, Profile replacement) {
        List<Profile> profiles = model.getProfiles();
        if (profiles == null) {
            profiles = new ArrayList<>();
            model.setProfiles(profiles);
        }
        for (Profile profile : profiles) {
            if (profile.getId().equals(DOCKER_PROFILE_ID)) {
                profile.setId(RENAMED_DOCKER_PROFILE_ID);
            }
        }
        profiles.add(replacement);
    }

    private void updateJobPomProperties(Model model, String key, String value) {
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
            model.setProperties(properties);
        }
        properties.setProperty(key, value);
    }

    private void updateJobCustomTemplateFile(IProject project, ERepositoryObjectType type, Profile templateProfile)
            throws Exception {
        IFolder folder = ResourceUtils.getFolder(project, type.getFolder(), false);
        File processFolder = folder.getLocation().toFile();
        List<File> allTemplates = new ArrayList<>();
        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                return file.isDirectory() || fileName.equals(TalendMavenConstants.POM_FILE_NAME);
            }

        };
        getAllCustomTemplates(processFolder.listFiles(filter), allTemplates, filter);
        for (File template : allTemplates) {
            Model model = MODEL_MANAGER.readMavenModel(template);

            replaceDockerProfile(model, templateProfile);
            updateJobPomProperties(model, "talend.job.folder", "@JobFolder@"); //$NON-NLS-1$ //$NON-NLS-2$

            PomUtil.savePom(null, model, template);
        }
    }

    private void getAllCustomTemplates(File[] files, List<File> allTemplates, FileFilter filter) {
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getAllCustomTemplates(file.listFiles(filter), allTemplates, filter);
                } else {
                    allTemplates.add(file);
                }
            }
        }
    }

    /**
     * only standard and bigdata jobs
     */
    private List<ERepositoryObjectType> getTypesOfProcess() {
        List<ERepositoryObjectType> allTypes = new ArrayList<ERepositoryObjectType>();

        if (ERepositoryObjectType.PROCESS != null) {
            allTypes.add(ERepositoryObjectType.PROCESS);
        }
        if (ERepositoryObjectType.PROCESS_MR != null) {
            allTypes.add(ERepositoryObjectType.PROCESS_MR);
        }
        if (ERepositoryObjectType.PROCESS_STORM != null) {
            allTypes.add(ERepositoryObjectType.PROCESS_STORM);
        }
        if (ERepositoryObjectType.PROCESS_SPARK != null) {
            allTypes.add(ERepositoryObjectType.PROCESS_SPARK);
        }
        if (ERepositoryObjectType.PROCESS_SPARKSTREAMING != null) {
            allTypes.add(ERepositoryObjectType.PROCESS_SPARKSTREAMING);
        }

        return allTypes;
    }

}
