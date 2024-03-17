package com.xiaodingsiren.beanutilshelper;

import cn.hutool.core.collection.CollStreamUtil;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author Rose Ding
 * @since 2024/3/17 13:52
 */
public class BeanUtilsInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public ProblemDescriptor[] checkMethod(@NotNull PsiMethod psiMethod, @NotNull InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> problems = new ArrayList<>();

        PsiCodeBlock body = psiMethod.getBody();
        if (body == null) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String canonicalText = methodExpression.getCanonicalText();
                if (canonicalText.startsWith(BeanUtilHelper.BEAN_UTIL_COPY_PROPERTIES) || canonicalText.startsWith(BeanUtilHelper.BEAN_UTILS_COPY_PROPERTIES)) {
                    BeanUtilHelper.Result invoke = BeanUtilHelper.invoke(expression);

                    PsiClass sourceClass = invoke.sourceClass();
                    PsiClass targetClass = invoke.targetClass();

                    List<BeanUtilHelper.Property> sourceProperties = invoke.sourceProperties();
                    List<BeanUtilHelper.Property> targetProperties = invoke.targetProperties();

                    Map<String, BeanUtilHelper.Property> targetPropertyMap = CollStreamUtil.toIdentityMap(targetProperties, BeanUtilHelper.Property::name);

                    Set<String> commonPropertyNames = BeanUtilHelper.findCommonPropertyNames(invoke);
                    List<String> ignoreProperties = invoke.ignoredProperties();
                    if (ignoreProperties.size() > 0) {
                        ignoreProperties.forEach(commonPropertyNames::remove);
                    }

                    if (commonPropertyNames.size() == 0) {
                        // 创建一个 ProblemDescriptor 来描述我们发现的问题
                        ProblemDescriptor problem = manager.createProblemDescriptor(
                                expression,
                                sourceClass.getName() + "与" + targetClass.getName() + "没有相同的属性",
                                true,
                                ProblemHighlightType.WEAK_WARNING,
                                isOnTheFly
                        );
                        problems.add(problem);
                    }


                    List<BeanUtilHelper.Property> sameNameNotSameType = sourceProperties.stream().filter(s -> s.mark().equals(BeanUtilHelper.Mark.SAME_NAME_NOT_SAME_TYPE)).toList();

                    if (sameNameNotSameType.size() > 0) {
                        String tips = sameNameNotSameType.stream().map(s -> s.toString() + s.mark().icon + targetPropertyMap.get(s.name()).toString()).collect(Collectors.joining("\n"));
                        ProblemDescriptor problem = manager.createProblemDescriptor(
                                expression,
                                sourceClass.getName() + "与" + targetClass.getName() + "有同名但不同类型的属性:\n" + tips,
                                true,
                                ProblemHighlightType.WEAK_WARNING,
                                isOnTheFly
                        );
                        problems.add(problem);
                    }
                }
            }
        });

        return problems.toArray(new ProblemDescriptor[0]);
    }
}
