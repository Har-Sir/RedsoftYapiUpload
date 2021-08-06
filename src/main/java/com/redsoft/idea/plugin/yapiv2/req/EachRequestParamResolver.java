package com.redsoft.idea.plugin.yapiv2.req;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.redsoft.idea.plugin.yapiv2.model.YApiParam;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * <b>参数逐个解析</b>
 * @author aqiu 2020/7/23 4:14 下午
 **/
public interface EachRequestParamResolver extends RequestParamResolver {

    default void doResolve(@NotNull PsiMethod m,
            @NotNull List<PsiParameter> parameterList,
            @NotNull YApiParam target) {
        parameterList.forEach(p -> this.doResolverItem(m, p, target));
    }

    void doResolverItem(@NotNull PsiMethod m, @NotNull PsiParameter param,
            @NotNull YApiParam target);
}
