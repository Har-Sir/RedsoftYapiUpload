package com.github.aqiu202.ideayapi.parser.support;

import com.github.aqiu202.ideayapi.model.ValueWrapper;
import com.github.aqiu202.ideayapi.model.YApiParam;
import com.intellij.psi.PsiMethod;

/**
 * Yapi接口信息的扩展支持
 */
public interface YApiSupport {

    default int getOrder() {
        return 0;
    }

    void handleMethod(PsiMethod psiMethod, YApiParam apiDTO);

    void handleParam(ValueWrapper wrapper);

    void handleField(ValueWrapper wrapper);
}