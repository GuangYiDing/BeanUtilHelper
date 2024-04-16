package com.xiaodingsiren.beanutilshelper.strategy.impl;

import cn.hutool.core.collection.CollStreamUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.xiaodingsiren.beanutilshelper.BeanUtilHelper;
import com.xiaodingsiren.beanutilshelper.strategy.ResolveStrategy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rose Ding
 * @since 2024/4/16 下午2:19
 */
public class ResolveDefaultStrategyImpl implements ResolveStrategy {
    @Override
    public String qualifiedName() {
        return "BeanUtil";
    }

    /**
     * 默认支持解析以下方法,也就是 Hutool 的方法
     * copyProperties(Object source, Object target, String... ignoreProperties)
     * copyProperties(Object source, Class<T> tClass, String... ignoreProperties)
     */
    @Override
    public BeanUtilHelper.Result resolve(PsiMethodCallExpression methodCallExpression) {
        PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
        PsiClass sourceClass = PsiUtil.resolveClassInType(expressions[0].getType());
        PsiClass targetClass = PsiUtil.resolveClassInType(expressions[1].getType());

        // 处理 Class<T> tClass 参数
        if (expressions[1] instanceof PsiClassObjectAccessExpression) {
            targetClass = PsiTypesUtil.getPsiClass(((PsiTypeElementImpl) expressions[1].getFirstChild()).getType());
        }

        if (sourceClass == null || targetClass == null) {
            return null;
        }

        // 处理 ignoreProperties 参数
        List<String> ignoreProperties = BeanUtilHelper.getIgnoreProperties(expressions);
        // 先收集一遍
        List<BeanUtilHelper.Property> sourceProperties = Stream.of(sourceClass.getAllFields())
                .map(s -> new BeanUtilHelper.Property(s.getName(), s.getType(), null)).toList();
        List<BeanUtilHelper.Property> targetProperties = Stream.of(targetClass.getAllFields())
                .map(s -> new BeanUtilHelper.Property(s.getName(), s.getType(), null)).toList();

        Map<String, BeanUtilHelper.Property> targetPropertyMap = CollStreamUtil.toIdentityMap(targetProperties, BeanUtilHelper.Property::name);
        Map<String, BeanUtilHelper.Property> sourcePropertyMap = CollStreamUtil.toIdentityMap(sourceProperties, BeanUtilHelper.Property::name);

        // 再标记一遍
        sourceProperties = sourceProperties.stream().map(s -> BeanUtilHelper.markProperties(ignoreProperties, targetPropertyMap, s)).collect(Collectors.toList());
        targetProperties = targetProperties.stream().map(s -> BeanUtilHelper.markProperties(ignoreProperties, sourcePropertyMap, s)).collect(Collectors.toList());

        return new BeanUtilHelper.Result(sourceClass, targetClass, sourceProperties, targetProperties, ignoreProperties);
    }
}
