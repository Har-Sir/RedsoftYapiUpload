package com.github.aqiu202.ideayapi.action;

import com.github.aqiu202.ideayapi.config.impl.ProjectConfigReader;
import com.github.aqiu202.ideayapi.config.xml.YApiProjectProperty;
import com.github.aqiu202.ideayapi.constant.YApiConstants;
import com.github.aqiu202.ideayapi.model.YApiParam;
import com.github.aqiu202.ideayapi.model.YApiResponse;
import com.github.aqiu202.ideayapi.model.YApiSaveParam;
import com.github.aqiu202.ideayapi.parser.PsiMethodParser;
import com.github.aqiu202.ideayapi.parser.YApiParser;
import com.github.aqiu202.ideayapi.parser.impl.PsiMethodParserImpl;
import com.github.aqiu202.ideayapi.parser.support.YApiSupportHolder;
import com.github.aqiu202.ideayapi.upload.YApiUpload;
import com.github.aqiu202.ideayapi.util.CollectionUtils;
import com.github.aqiu202.ideayapi.util.NotificationUtils;
import com.github.aqiu202.ideayapi.util.PropertiesReader;
import com.github.aqiu202.ideayapi.util.StringUtils;
import com.intellij.notification.NotificationListener.UrlOpeningListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.PsiFileImplUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>事件类，所有的解析动作的起点 {@link #actionPerformed}</b>
 *
 * @author aqiu 2020/7/24 9:24 上午
 **/
public class YApiUploadAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            Project project = e.getData(CommonDataKeys.PROJECT);
            YApiSupportHolder.init(project);

            YApiProjectProperty property = ProjectConfigReader.read(project);
            String token = property.getToken();
            // 项目ID
            int projectId = property.getProjectId();
            // yapi地址
            String yapiUrl = property.getUrl();
            // 配置校验
            if (StringUtils.isEmpty(token) || StringUtils.isEmpty(yapiUrl) || projectId <= 0) {
                NotificationUtils.createNotification("配置信息异常", "请检查配置参数是否正常",
                        NotificationType.ERROR).notify(project);
                return;
            }
            String prefixPath = "";
            String catId = null;
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if(psiFile != null){
                //Messages.showInfoMessage(psiFile.getName() , psiFile.getText());
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if(virtualFile != null){
                    String filePath =  virtualFile.getPath();
                    //Messages.showInfoMessage(virtualFile.getName() ,filePath);
                    if (filePath.contains("src/main/java")) {
                        int index = filePath.indexOf("src/main/java");
                        filePath = filePath.substring(0, index);
                        filePath = filePath + "src/main/resources/yapi.properties";
                        Map<String,String> map = PropertiesReader.read(filePath);
                        prefixPath = map.getOrDefault("yapi.setting.url.prefix","");
                        catId = map.getOrDefault("yapi.setting.catId",null);
                        token = map.getOrDefault("yapi.setting.token", token);
                        projectId = Integer.parseInt(map.getOrDefault("yapi.setting.projectId","" + projectId));

                        property.setToken(token);
                        property.setProjectId(projectId);
                        //Messages.showInfoMessage(virtualFile.getName() + "读取配置" ,prefixPath);
                    }
                }
            }



            PsiMethodParser methodParser = new PsiMethodParserImpl(property, project);
            //获得api 需上传的接口列表 参数对象
            Set<YApiParam> yApiParams = new YApiParser(project, methodParser).parse(e);
            if (CollectionUtils.isNotEmpty(yApiParams)) {
                Set<YApiSaveParam> allSaveParams = new HashSet<>();
                for (YApiParam yApiParam : yApiParams) {
                    allSaveParams.addAll(yApiParam.convert(prefixPath));
                }
                this.setDefaultInfo(allSaveParams, token);
                Map<String, Set<YApiSaveParam>> groupedParams = allSaveParams.stream().filter(param -> StringUtils.isNotBlank(param.getMenu()))
                        .collect(Collectors.groupingBy(YApiSaveParam::getMenu, Collectors.toSet()));
                for (Map.Entry<String, Set<YApiSaveParam>> entry : groupedParams.entrySet()) {
                    Set<YApiSaveParam> params = entry.getValue();
                    for (YApiSaveParam param : params) {
                        try {
                            // 设置模块id
                            if(catId != null){
                                param.setCatid(catId);
                            }
                            // 上传
                            YApiResponse yapiResponse = new YApiUpload()
                                    .uploadSave(property, param, project.getBasePath());
                            if (yapiResponse.getErrcode() != 0) {
                                NotificationUtils.createNotification("上传失败",
                                        "api上传失败原因:" + yapiResponse.getErrmsg(),
                                        NotificationType.ERROR).notify(project);
                            }
                        } catch (Exception e1) {
                            NotificationUtils.createNotification("上传失败", "api上传失败原因:" + e1,
                                            NotificationType.ERROR)
                                    .notify(project);
                            return;
                        }
                    }
                    String menu = entry.getKey();
                    String url = yapiUrl + "/project/" + projectId + "/interface/api/cat_"
                            + YApiUpload.findMenuByName(menu);
                    NotificationUtils.createNotification("上传成功",
                                    "<p>接口文档地址:  <a href=\"" + url + "\">" + url
                                            + "</a></p>",
                                    NotificationType.INFORMATION).setListener(new UrlOpeningListener(false))
                            .notify(project);
                }
            } else {
                NotificationUtils.createNotification("上传提示", "未发现符合条件的接口信息",
                                NotificationType.INFORMATION)
                        .notify(project);
            }
        } finally {
            YApiUpload.clearCache();
        }
    }

    private void setDefaultInfo(Collection<YApiSaveParam> yapiSaveParams, String token) {
        for (YApiSaveParam yapiSaveParam : yapiSaveParams) {
            yapiSaveParam.setToken(token);
            if (StringUtils.isEmpty(yapiSaveParam.getMenu())) {
                yapiSaveParam.setMenu(YApiConstants.menu);
            }
        }
    }

}
