package com.redsoft.idea.plugin.yapiv2.parser;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiMethod;
import com.redsoft.idea.plugin.yapiv2.base.DeprecatedAssert;
import com.redsoft.idea.plugin.yapiv2.base.impl.DeprecatedAssertImpl;
import com.redsoft.idea.plugin.yapiv2.constant.NotificationConstants;
import com.redsoft.idea.plugin.yapiv2.constant.YApiConstants;
import com.redsoft.idea.plugin.yapiv2.model.YApiParam;
import com.redsoft.idea.plugin.yapiv2.parser.impl.PsiClassParserImpl;
import com.redsoft.idea.plugin.yapiv2.util.PsiUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>接口信息解析入口</b>
 *
 * @author aqiu 2019-06-15 11:46
 **/
public class YApiParser {

    private final DeprecatedAssert deprecatedAssert = new DeprecatedAssertImpl();

    private final Project project;
    private final PsiMethodParser methodParser;
    private final PsiClassParser classParser;

    public YApiParser(Project project, PsiMethodParser methodParser) {
        this.project = project;
        this.methodParser = methodParser;
        this.classParser = new PsiClassParserImpl(methodParser);
    }

    public YApiParser(Project project, PsiMethodParser methodParser, PsiClassParser classParser) {
        this.project = project;
        this.methodParser = methodParser;
        this.classParser = classParser;
    }

    public Set<YApiParam> parse(AnActionEvent e) {
        Set<YApiParam> yApiParams = new HashSet<>();
        PsiMethod selectMethod = PsiUtils.getSelectMethod(e);
        // 如果选取的是方法
        if (selectMethod != null) {
            PsiClass currentClass = (PsiClass) selectMethod.getParent();
            //获取该方法是否已经标记过时
            if (this.deprecatedAssert.isDeprecated(currentClass, selectMethod)) {
                NotificationConstants.NOTIFICATION_GROUP
                        .createNotification(YApiConstants.name, "该类/方法已过时",
                                "该类/方法(或注释中)含有@Deprecated注解，如需上传，请删除该注解", NotificationType.WARNING)
                        .notify(project);
                return null;
            }
            YApiParam param = this.methodParser.parse(currentClass, selectMethod);
            yApiParams.add(param);
        }
        // 如果选取的是类
        PsiClass selectedClass = PsiUtils.getSelectClass(e);
        if (selectedClass != null) {
            //获取该类是否已经过时
            if (deprecatedAssert.isDeprecated(selectedClass)) {
                NotificationConstants.NOTIFICATION_GROUP
                        .createNotification(YApiConstants.name, "该类已过时",
                                "该类(或注释中)含有@Deprecated注解，如需上传，请删除该注解", NotificationType.WARNING)
                        .notify(project);
                return null;
            }
            List<YApiParam> params = this.classParser.parse(selectedClass);
            yApiParams.addAll(params);
        }
        PsiDirectory selectDir = PsiUtils.getSelectPackage(e);
        // 如果选取的是文件夹
        if (selectDir != null) {
            Set<PsiClass> classes = new HashSet<>();
            PsiUtils.collectClasses(selectDir, classes);
            classes.stream()
                    .filter(c -> !this.deprecatedAssert.isDeprecated(c))
                    .forEach(c -> yApiParams.addAll(this.classParser.parse(c)));

        }
        return yApiParams;
    }

}
