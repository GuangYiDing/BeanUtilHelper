package com.xiaodingsiren.beanutilshelper.strategy;

import com.intellij.psi.PsiMethodCallExpression;
import com.xiaodingsiren.beanutilshelper.BeanUtilHelper;

/**
 * @author Rose Ding
 * @since 2024/4/16 下午2:17
 */
public interface ResolveStrategy {

    /**
     * 是否支持
     */
    default boolean isSupport(PsiMethodCallExpression methodCallExpression) {
        return qualifiedName().equals(BeanUtilHelper.resolveQualifiedName(methodCallExpression));
    }

    /**
     * 全限类名
     */
    String qualifiedName();


    /**
     * 从 PsiMethodCallExpression 解析出结果
     */
    BeanUtilHelper.Result resolve(PsiMethodCallExpression methodCallExpression);

}
