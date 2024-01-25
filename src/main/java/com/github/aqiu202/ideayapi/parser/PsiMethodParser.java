package com.github.aqiu202.ideayapi.parser;

import com.github.aqiu202.ideayapi.model.YApiParam;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * <b>方法解析</b>
 *
 * @author aqiu 2020/5/12 11:02 上午
 **/
public interface PsiMethodParser {

    List<YApiParam> parse(@NotNull PsiClass c, @NotNull PsiMethod m);

}
