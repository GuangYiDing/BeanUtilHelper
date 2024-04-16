package com.xiaodingsiren.beanutilshelper.action;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.xiaodingsiren.beanutilshelper.BeanUtilHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Rose Ding
 * @since 2024/1/30 10:05
 */
public class ShowDiffPropertiesAction implements IntentionAction {


    @Override
    public @IntentionName @NotNull String getText() {
        return "BeanUtilHelper - Show different properties";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return BeanUtilHelper.BEAN_UTIL_HELPER;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        return BeanUtilHelper.BeanUtilHelperIsAvailable(editor, psiFile);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        // 实现添加注释的具体逻辑
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            int offset = editor.getCaretModel().getOffset();
            PsiElement elementAtCaret = psiFile.findElementAt(offset);
            PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethodCallExpression.class);
            if (methodCallExpression != null) {
                // 计算要插入注释的位置
                int lineNum = document.getLineNumber(methodCallExpression.getTextRange().getStartOffset());
                int lineStartOffset = document.getLineStartOffset(lineNum);
                // 缩进的位置
                String linePrefix = document.getText(new TextRange(lineStartOffset, methodCallExpression.getTextRange().getStartOffset()));

                BeanUtilHelper.Result result = BeanUtilHelper.invoke(editor, psiFile);
                if (result == null) {
                    return;
                }
                PsiClass sourceClass = result.sourceClass();
                PsiClass targetClass = result.targetClass();
                if (sourceClass == null || targetClass == null) {
                    return;
                }

                List<BeanUtilHelper.Property> sourceProperties = result.sourceProperties();
                List<BeanUtilHelper.Property> targetProperties = result.targetProperties();
                List<String> ignoredPropertiesName = result.ignoredProperties();

                List<BeanUtilHelper.Property> sameProperties = new ArrayList<>();
                List<BeanUtilHelper.Property> sameNameNotSameType = new ArrayList<>();
                List<BeanUtilHelper.Property> ignoredProperties = new ArrayList<>();
                List<BeanUtilHelper.Property> diffProperties = new ArrayList<>();

                Map<String, BeanUtilHelper.Property> targetPropertyMap = CollStreamUtil.toIdentityMap(targetProperties, BeanUtilHelper.Property::name);
                Map<String, BeanUtilHelper.Property> sourcePropertyMap = CollStreamUtil.toIdentityMap(sourceProperties, BeanUtilHelper.Property::name);

                for (BeanUtilHelper.Property sourceProperty : sourceProperties) {
                    switch (sourceProperty.mark()) {
                        case SAME -> sameProperties.add(sourceProperty);
                        case SAME_NAME_NOT_SAME_TYPE -> sameNameNotSameType.add(sourceProperty);
                        case IGNORED -> ignoredProperties.add(sourceProperty);
                        case DIFF -> diffProperties.add(sourceProperty);
                    }
                }
                for (BeanUtilHelper.Property targetProperty : targetProperties) {
                    switch (targetProperty.mark()) {
                        case IGNORED -> {
                            if (ignoredProperties.stream().noneMatch(s -> s.name().equals(targetProperty.name()))) {
                                ignoredProperties.add(targetProperty);
                            }
                        }
                        case DIFF -> diffProperties.add(targetProperty);
                    }
                }

                String sourceClassName = sourceClass.getName();
                String targetClassName = targetClass.getName();
                // 同属性
                String commonPropertiesComment = sameProperties.stream()
                        .map(s -> s + s.mark().icon + s)
                        .collect(Collectors.joining("\n"));
                // 同属性不同类型
                String sameNameButNotSameTypePropertiesHtml = sameNameNotSameType.stream()
                        .map(s -> {
                            BeanUtilHelper.Property targerProperty = targetPropertyMap.get(s.name());
                            // 类名相同,类路径不同
                            if (s.getShortType().equals(targerProperty.getShortType())) {
                                return s.toFullString() + s.mark().icon + s.toFullString();
                            }
                            return s + s.mark().icon + targerProperty;
                        })
                        .collect(Collectors.joining("\n"));
                // 忽略的属性
                String ignoredPropertiesComment = ignoredProperties.stream()
                        .map(s -> {
                            StringBuilder htmlBuilder = new StringBuilder();
                            if (sourcePropertyMap.containsKey(s.name())) {
                                htmlBuilder.append("~").append(sourcePropertyMap.get(s.name()).toString()).append("~");
                            } else {
                                htmlBuilder.append(" ".repeat(targetPropertyMap.get(s.name()).toString().length()));
                            }
                            htmlBuilder.append(s.mark().icon);
                            if (targetPropertyMap.containsKey(s.name())) {
                                htmlBuilder.append("~").append(targetPropertyMap.get(s.name()).toString()).append("~");
                            }
                            return htmlBuilder.toString();
                        })
                        .collect(Collectors.joining("\n"));
                // 不同的属性
                String diffPropertiesComment = diffProperties.stream()
                        .map(s -> {
                            StringBuilder htmlBuilder = new StringBuilder();
                            if (sourcePropertyMap.containsKey(s.name())) {
                                htmlBuilder.append(sourcePropertyMap.get(s.name()).toString());
                            } else {
                                htmlBuilder.append(" ".repeat(targetPropertyMap.get(s.name()).toString().length()));
                            }
                            htmlBuilder.append(s.mark().icon);
                            if (targetPropertyMap.containsKey(s.name())) {
                                htmlBuilder.append(targetPropertyMap.get(s.name()).toString());
                            }
                            return htmlBuilder.toString();
                        })
                        .collect(Collectors.joining("\n"));
                String comment =
                        StrUtil.format("""
                                        /* 
                                        差异对比
                                        {} ➡️ {}                               
                                        {}
                                        {}
                                        {}
                                        {}
                                        */
                                        """,
                                sourceClassName,
                                targetClassName,
                                commonPropertiesComment,
                                sameNameButNotSameTypePropertiesHtml,
                                ignoredPropertiesComment,
                                diffPropertiesComment);
                // 将注释与原代码的缩进对齐
                String commentWithIndent = comment.lines().filter(StrUtil::isNotBlank).map(s -> linePrefix + s).collect(Collectors.joining("\n")) + "\n";
                document.insertString(lineStartOffset, commentWithIndent);
                PsiDocumentManager.getInstance(project).commitDocument(document);

            }
        });
    }


    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        BeanUtilHelper.Result result = BeanUtilHelper.invoke(editor, file);
        if (result == null) {
            return IntentionPreviewInfo.EMPTY;
        }

        PsiClass sourceClass = result.sourceClass();
        PsiClass targetClass = result.targetClass();

        if (sourceClass == null || targetClass == null) {
            return IntentionPreviewInfo.EMPTY;
        }

        List<BeanUtilHelper.Property> sourceProperties = result.sourceProperties();
        List<BeanUtilHelper.Property> targetProperties = result.targetProperties();
        List<String> ignoredPropertiesName = result.ignoredProperties();

        List<BeanUtilHelper.Property> sameProperties = new ArrayList<>();
        List<BeanUtilHelper.Property> sameNameNotSameType = new ArrayList<>();
        List<BeanUtilHelper.Property> ignoredProperties = new ArrayList<>();
        List<BeanUtilHelper.Property> diffProperties = new ArrayList<>();

        Map<String, BeanUtilHelper.Property> targetPropertyMap = CollStreamUtil.toIdentityMap(targetProperties, BeanUtilHelper.Property::name);
        Map<String, BeanUtilHelper.Property> sourcePropertyMap = CollStreamUtil.toIdentityMap(sourceProperties, BeanUtilHelper.Property::name);

        for (BeanUtilHelper.Property sourceProperty : sourceProperties) {
            switch (sourceProperty.mark()) {
                case SAME -> sameProperties.add(sourceProperty);
                case SAME_NAME_NOT_SAME_TYPE -> sameNameNotSameType.add(sourceProperty);
                case IGNORED -> ignoredProperties.add(sourceProperty);
                case DIFF -> diffProperties.add(sourceProperty);
            }
        }
        for (BeanUtilHelper.Property targetProperty : targetProperties) {
            switch (targetProperty.mark()) {
                case IGNORED -> {
                    if (ignoredProperties.stream().noneMatch(s -> s.name().equals(targetProperty.name()))) {
                        ignoredProperties.add(targetProperty);
                    }
                }
                case DIFF -> diffProperties.add(targetProperty);
            }
        }

        String sourceClassName = sourceClass.getName();
        String targetClassName = targetClass.getName();
        // 同属性
        String commonPropertiesHtml = sameProperties.stream()
                .map(s -> StrUtil.format("<li style=\"color: {}\">", s.mark().color) + s + s.mark().icon + s + "</li>")
                .collect(Collectors.joining("\n"));
        // 同属性不同类型
        String sameNameButNotSameTypePropertiesHtml = sameNameNotSameType.stream()
                .map(s -> {
                    BeanUtilHelper.Property targerProperty = targetPropertyMap.get(s.name());
                    // 类名相同,类路径不同
                    if (s.getShortType().equals(targerProperty.getShortType())) {
                        return StrUtil.format("<li style=\"color: {}\">", s.mark().color) + s.toFullString() + s.mark().icon + s.toFullString() + "</li>";
                    }
                    return StrUtil.format("<li style=\"color: {}\">", s.mark().color) + s + s.mark().icon + targerProperty + "</li>";
                })
                .collect(Collectors.joining("\n"));
        // 忽略的属性
        String ignoredPropertiesHtml = ignoredProperties.stream()
                .map(s -> {
                    StringBuilder htmlBuilder = new StringBuilder(StrUtil.format("<li style=\"color: {}\">", s.mark().color));
                    if (sourcePropertyMap.containsKey(s.name())) {
                        htmlBuilder.append("<s>").append(sourcePropertyMap.get(s.name()).toString()).append("</s>");
                    } else {
                        htmlBuilder.append("&nbsp;".repeat(targetPropertyMap.get(s.name()).toString().length()));
                    }
                    htmlBuilder.append(s.mark().icon);
                    if (targetPropertyMap.containsKey(s.name())) {
                        htmlBuilder.append("<s>").append(targetPropertyMap.get(s.name()).toString()).append("</s>");
                    }
                    htmlBuilder.append("</li>");
                    return htmlBuilder.toString();
                })
                .collect(Collectors.joining("\n"));
        // 不同的属性
        String diffPropertiesHtml = diffProperties.stream()
                .map(s -> {
                    StringBuilder htmlBuilder = new StringBuilder(StrUtil.format("<li style=\"color: {}\">", s.mark().color));
                    if (sourcePropertyMap.containsKey(s.name())) {
                        htmlBuilder.append(sourcePropertyMap.get(s.name()).toString());
                    } else {
                        htmlBuilder.append("&nbsp;".repeat(targetPropertyMap.get(s.name()).toString().length()));
                    }
                    htmlBuilder.append(s.mark().icon);
                    if (targetPropertyMap.containsKey(s.name())) {
                        htmlBuilder.append(targetPropertyMap.get(s.name()).toString());
                    }
                    htmlBuilder.append("</li>");
                    return htmlBuilder.toString();
                })
                .collect(Collectors.joining("\n"));
        String html =
                StrUtil.format("""
                                <html lang="zh-Cn">
                                <body>
                                {} ➡️ {}
                                <ul>
                                  {}
                                  {}
                                  {}
                                  {}
                                </ul>
                                </body>
                                </html>
                                """,
                        sourceClassName,
                        targetClassName,
                        commonPropertiesHtml,
                        sameNameButNotSameTypePropertiesHtml,
                        ignoredPropertiesHtml,
                        diffPropertiesHtml);
        return new IntentionPreviewInfo.Html(html);
    }


    @Override
    public boolean startInWriteAction() {
        return true;
    }

}
