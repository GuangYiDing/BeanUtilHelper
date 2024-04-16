package com.xiaodingsiren.beanutilshelper.action;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.navigation.NavigationItem;
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
public class ShowCopyPropertiesAction implements IntentionAction {

    public static final String NO_COMMON_PROPERTIES_FOUND = "没有找到共有的属性";

    @Override
    public @IntentionName @NotNull String getText() {
        return "BeanUtilHelper - Show copy properties";
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
                BeanUtilHelper.Result result = BeanUtilHelper.invoke(methodCallExpression);

                if (result == null) {
                    return;
                }

                PsiClass sourceClass = result.sourceClass();
                PsiClass targetClass = result.targetClass();

                if (sourceClass == null || targetClass == null) {
                    return;
                }
                Set<String> commonPropertyNames = findCommonPropertyNames(sourceClass, targetClass);

                // 处理 ignoreProperties 参数
                List<String> ignoreProperties = result.ignoredProperties();
                if (!ignoreProperties.isEmpty()) {
                    ignoreProperties.forEach(commonPropertyNames::remove);
                }

                if (commonPropertyNames.isEmpty()) {
                    HintManager.getInstance().showInformationHint(editor, NO_COMMON_PROPERTIES_FOUND);
                    return;
                }

                // 计算要插入注释的位置
                int lineNum = document.getLineNumber(methodCallExpression.getTextRange().getStartOffset());
                int lineStartOffset = document.getLineStartOffset(lineNum);
                // 缩进的位置
                String linePrefix = document.getText(new TextRange(lineStartOffset, methodCallExpression.getTextRange().getStartOffset()));
                String commentText;
                // 共有属性超过四个使用块注释显示
                if (commonPropertyNames.size() > 4) {
                    commentText = commonPropertyNames.stream()
                            .map(propertyName -> linePrefix + "\t\t" + propertyName)
                            .collect(Collectors.joining(",\n",
                                    String.format("""
                                            /*
                                            %s    从 %s 对象中复制属性:
                                            """, linePrefix, sourceClass.getName()),
                                    String.format("""
                                                                                        
                                            %s    到 %s 对象中
                                            %s*/""", linePrefix, targetClass.getName(), linePrefix)));
                } else {
                    commentText = commonPropertyNames.stream().collect(Collectors.joining(",",
                            String.format("// 从 %s 对象中复制属性: ", sourceClass.getName()),
                            String.format(" 到 %s 对象中", targetClass.getName())));
                }
                // 将注释与原代码的缩进对齐
                String commentWithIndent = linePrefix + commentText + '\n';
                document.insertString(lineStartOffset, commentWithIndent);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            }
        });
    }

    private Set<String> findCommonPropertyNames(PsiClass sourceClass, PsiClass targetClass) {
        Set<String> sourceClassFields = Arrays.stream(sourceClass.getAllFields()).map(NavigationItem::getName).collect(Collectors.toSet());
        Set<String> targetClassFields = Arrays.stream(targetClass.getAllFields()).map(NavigationItem::getName).collect(Collectors.toSet());
        sourceClassFields.retainAll(targetClassFields);
        return sourceClassFields.stream().sorted().collect(Collectors.toCollection(TreeSet::new));
    }



    @Override
    public boolean startInWriteAction() {
        return true;
    }


    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        BeanUtilHelper.Result result = BeanUtilHelper.invoke(editor, file);
        if (result == null) {
            return IntentionPreviewInfo.EMPTY;
        }
        PsiClass sourceClass = result.sourceClass();
        PsiClass targetClass = result.targetClass();
        String commentText;
        Set<String> commonPropertyNames = BeanUtilHelper.findCommonPropertyNames(result);
        // 处理 ignoreProperties 参数
        List<String> ignoreProperties = result.ignoredProperties();
        if (!ignoreProperties.isEmpty()) {
            ignoreProperties.forEach(commonPropertyNames::remove);
        }
        if (commonPropertyNames.isEmpty()) {
            return new IntentionPreviewInfo.Html(NO_COMMON_PROPERTIES_FOUND);
        }
        // 共有属性超过四个使用块注释显示
        if (commonPropertyNames.size() > 4) {
            commentText = commonPropertyNames.stream()
                    .collect(Collectors.joining(",\n",
                            String.format("""
                                    /*
                                       从 %s 对象中复制属性:
                                    """, sourceClass.getName()),
                            String.format("""
                                                                        
                                       到 %s 对象中
                                    */""", targetClass.getName())));
        } else {
            commentText = commonPropertyNames.stream().collect(Collectors.joining(",",
                    String.format("// 从 %s 对象中复制属性: ", sourceClass.getName()),
                    String.format(" 到 %s 对象中", targetClass.getName())));
        }
        return new IntentionPreviewInfo.Html(commentText);
    }

}
