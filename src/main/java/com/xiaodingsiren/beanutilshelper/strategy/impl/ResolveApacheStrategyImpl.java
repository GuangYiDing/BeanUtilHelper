package com.xiaodingsiren.beanutilshelper.strategy.impl;

import cn.hutool.core.collection.CollStreamUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
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
public class ResolveApacheStrategyImpl implements ResolveStrategy {
    @Override
    public String qualifiedName() {
        return "org.apache.commons.beanutils.BeanUtils";
    }

    @Override
    public BeanUtilHelper.Result resolve(PsiMethodCallExpression methodCallExpression) {
        PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
        // apache 的 源类型 和目标类型 是反着来的
        PsiClass sourceClass = PsiUtil.resolveClassInType(expressions[1].getType());
        PsiClass targetClass = PsiUtil.resolveClassInType(expressions[0].getType());

        if (sourceClass == null || targetClass == null) {
            return null;
        }

        // apache 没有 ignoreProperties 参数
        List<String> ignoreProperties = List.of();
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
