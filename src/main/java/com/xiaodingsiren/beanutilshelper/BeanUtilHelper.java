package com.xiaodingsiren.beanutilshelper;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.xiaodingsiren.beanutilshelper.strategy.ResolveStrategy;
import com.xiaodingsiren.beanutilshelper.strategy.impl.ResolveApacheStrategyImpl;
import com.xiaodingsiren.beanutilshelper.strategy.impl.ResolveDefaultStrategyImpl;
import com.xiaodingsiren.beanutilshelper.strategy.impl.ResolveHutoolStrategyImpl;
import com.xiaodingsiren.beanutilshelper.strategy.impl.ResolveSpringStrategyImpl;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rose Ding
 * @since 2024/3/15 15:46
 */
public class BeanUtilHelper {

    public static final String BEAN_UTIL_COPY_PROPERTIES = "BeanUtil.copyProperties";
    public static final String BEAN_UTILS_COPY_PROPERTIES = "BeanUtils.copyProperties";
    public static final String BEAN_UTIL_HELPER = "BeanUtilHelper";

    public static final List<ResolveStrategy> resolveStrategies = List.of(
            new ResolveApacheStrategyImpl(),
            new ResolveSpringStrategyImpl(),
            new ResolveHutoolStrategyImpl()
    );




    public static boolean BeanUtilHelperIsAvailable(Editor editor, PsiFile psiFile) {
        if (!(psiFile instanceof PsiJavaFile)) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAtCaret = psiFile.findElementAt(offset);

        if (elementAtCaret == null) {
            return false;
        }

        PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethodCallExpression.class);


        if (methodCallExpression == null) {
            return false;
        }
        String canonicalText = methodCallExpression.getMethodExpression().getCanonicalText();
        return canonicalText.startsWith(BEAN_UTIL_COPY_PROPERTIES) ||
               canonicalText.startsWith(BEAN_UTILS_COPY_PROPERTIES) ||
               resolveStrategies.stream()
                       .map(resolveStrategy -> resolveStrategy.qualifiedName() + ".copyProperties")
                       .anyMatch(canonicalText::startsWith);
    }


    public static String resolveQualifiedName(PsiMethodCallExpression methodCallExpression) {
        PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
        PsiElement resolvedMethod = methodExpression.resolve();
        if (resolvedMethod instanceof PsiMethod method) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                return containingClass.getQualifiedName();
            }
        }
        return null;
    }



    public static Result invoke(PsiMethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return null;
        }
        for (ResolveStrategy resolveStrategy : resolveStrategies) {
            if (resolveStrategy.isSupport(methodCallExpression)) {
                return resolveStrategy.resolve(methodCallExpression);
            }
        }
        return new ResolveDefaultStrategyImpl().resolve(methodCallExpression);
    }

    public static Result invoke(Editor editor, PsiFile psiFile) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethodCallExpression.class);
        return invoke(methodCallExpression);
    }

    @NotNull
    public static Property markProperties(List<String> ignoreProperties, Map<String, Property> propertyMap, Property s) {
        Mark mark;
        // 是否是忽略的
        if (ignoreProperties.contains(s.name)) {
            mark = Mark.IGNORED;
        } else {
            // 是否同名
            if (propertyMap.containsKey(s.name)) {
                // 是否同类型
                if (s.equals(propertyMap.get(s.name))) {
                    mark = Mark.SAME;
                } else {
                    mark = Mark.SAME_NAME_NOT_SAME_TYPE;
                }
            } else {
                mark = Mark.DIFF;
            }
        }
        return new Property(s.name, s.type, mark);
    }

    public static Set<String> findCommonPropertyNames(Result result) {
        Set<String> sourceClassFields = result.sourceProperties.stream().map(Property::name).collect(Collectors.toSet());
        Set<String> targetClassFields = result.targetProperties.stream().map(Property::name).collect(Collectors.toSet());
        sourceClassFields.retainAll(targetClassFields);
        return sourceClassFields.stream().sorted().collect(Collectors.toCollection(TreeSet::new));
    }


    public static List<String> getIgnoreProperties(PsiExpression[] expressions) {
        if (expressions.length > 2) {
            return Stream.of(expressions)
                    .filter(expression -> expression instanceof PsiLiteralExpression)
                    .map(e -> e.getText().replace("\"", "")).collect(Collectors.toList());
        }
        return List.of();
    }


    @Getter
    public enum Mark {
        SAME("green", " ✅ "),
        SAME_NAME_NOT_SAME_TYPE("orange", " ❓ "),
        IGNORED("gray", " 🚫 "),
        DIFF("gray", " ❌ "),
        ;
        public final String color;
        public final String icon;

        Mark(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    public record Property(String name, PsiType type, Mark mark) {
        @Override
        public String toString() {
            return this.getShortType() + " " + name;
        }

        public String toFullString() {
            return type.getCanonicalText() + " " + name;
        }

        public String getShortType() {
            return type.getCanonicalText().substring(type.getCanonicalText().lastIndexOf(".") + 1);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Property property) {
                return name.equals((property).name) && type.getCanonicalText().equals((property).type.getCanonicalText());
            }
            return false;
        }

    }

    public record Result(PsiClass sourceClass, PsiClass targetClass,
                  List<Property> sourceProperties, List<Property> targetProperties, List<String> ignoredProperties) {
    }

}
