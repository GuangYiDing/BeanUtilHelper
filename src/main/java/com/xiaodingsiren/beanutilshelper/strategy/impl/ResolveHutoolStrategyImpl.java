package com.xiaodingsiren.beanutilshelper.strategy.impl;

import com.intellij.psi.PsiMethodCallExpression;
import com.xiaodingsiren.beanutilshelper.BeanUtilHelper;
import com.xiaodingsiren.beanutilshelper.strategy.ResolveStrategy;

/**
 * @author Rose Ding
 * @since 2024/4/16 下午2:19
 */
public class ResolveHutoolStrategyImpl implements ResolveStrategy {
    @Override
    public String qualifiedName() {
        return "cn.hutool.core.bean.BeanUtil";
    }

    private final ResolveStrategy defaultStrategy = new ResolveDefaultStrategyImpl();

    @Override
    public BeanUtilHelper.Result resolve(PsiMethodCallExpression methodCallExpression) {
        return defaultStrategy.resolve(methodCallExpression);
    }
}
