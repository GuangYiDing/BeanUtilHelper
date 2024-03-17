package com.xiaodingsiren.beanutilshelper;

import cn.hutool.core.collection.CollStreamUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
        return canonicalText.startsWith(BEAN_UTIL_COPY_PROPERTIES) || canonicalText.startsWith(BEAN_UTILS_COPY_PROPERTIES);
    }

    public static Result invoke(Editor editor, PsiFile psiFile) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethodCallExpression.class);
        if (methodCallExpression == null) {
            return null;
        }
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
        List<String> ignoreProperties = getIgnoreProperties(expressions);
        // 先收集一遍
        List<Property> sourceProperties = Stream.of(sourceClass.getAllFields())
                .map(s -> new Property(s.getName(), s.getType(), null)).toList();
        List<Property> targetProperties = Stream.of(targetClass.getAllFields())
                .map(s -> new Property(s.getName(), s.getType(), null)).toList();

        Map<String, BeanUtilHelper.Property> targetPropertyMap = CollStreamUtil.toIdentityMap(targetProperties, BeanUtilHelper.Property::name);
        Map<String, BeanUtilHelper.Property> sourcePropertyMap = CollStreamUtil.toIdentityMap(sourceProperties, BeanUtilHelper.Property::name);

        // 再标记一遍
        sourceProperties = sourceProperties.stream().map(s -> markProperties(ignoreProperties, targetPropertyMap, s)).collect(Collectors.toList());
        targetProperties = targetProperties.stream().map(s -> markProperties(ignoreProperties, sourcePropertyMap, s)).collect(Collectors.toList());

        return new Result(sourceClass, targetClass, sourceProperties, targetProperties, ignoreProperties);
    }

    @NotNull
    private static Property markProperties(List<String> ignoreProperties, Map<String, Property> propertyMap, Property s) {
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


    enum Mark {
        SAME("green", " ✅ "),
        SAME_NAME_NOT_SAME_TYPE("orange", " ❓ "),
        IGNORED("gray", " 🚫 "),
        DIFF("gray", " ❌ "),
        ;
        @Getter
        final String color;
        @Getter
        final String icon;

        Mark(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    record Property(String name, PsiType type, Mark mark) {
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

    record Result(PsiClass sourceClass, PsiClass targetClass,
                  List<Property> sourceProperties, List<Property> targetProperties, List<String> ignoredProperties) {
    }

}
