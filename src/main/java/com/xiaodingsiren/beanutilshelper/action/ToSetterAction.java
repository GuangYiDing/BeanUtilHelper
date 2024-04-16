package com.xiaodingsiren.beanutilshelper.action;

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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Rose Ding
 * @since 2024/3/17 20:15
 */
public class ToSetterAction implements IntentionAction {
    @Override
    public @IntentionName @NotNull String getText() {
        return "BeanUtilHelper - ToGetterSetter";
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
                BeanUtilHelper.Result invoke = BeanUtilHelper.invoke(methodCallExpression);
                if (invoke == null) {
                    return;
                }
                PsiClass sourceClass = invoke.sourceClass();
                PsiClass targetClass = invoke.targetClass();
                if (sourceClass == null || targetClass == null) {
                    return;
                }
                PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
                String sourceArgsName = expressions[0].getText();
                Set<String> commonPropertyNames = BeanUtilHelper.findCommonPropertyNames(invoke);
                List<String> ignoreProperties = invoke.ignoredProperties();
                if (!ignoreProperties.isEmpty()) {
                    ignoreProperties.forEach(commonPropertyNames::remove);
                }

                // 计算要插入注释的位置
                int lineNum = document.getLineNumber(methodCallExpression.getTextRange().getStartOffset());
                int lineStartOffset = document.getLineStartOffset(lineNum);
                int lineEndOffset = document.getLineEndOffset(lineNum);
                // 缩进的位置
                String linePrefix = document.getText(new TextRange(lineStartOffset, methodCallExpression.getTextRange().getStartOffset()));
                String codeText = linePrefix + targetClass.getName() + " target = new " + targetClass.getName() + "();\n";

                String collect = commonPropertyNames.stream().map(
                        propertyName -> {
                            // 将属性名的首字母大写
                            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                            String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                            return linePrefix + "target." + setterName + "(" + sourceArgsName + "." + getterName + "()" + ");";
                        }
                ).collect(Collectors.joining("\n"));

                String commentWithIndent = codeText + collect + '\n';
                document.replaceString(lineStartOffset, lineEndOffset, commentWithIndent);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            }
        });


    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
        BeanUtilHelper.Result result = BeanUtilHelper.invoke(editor, psiFile);
        if (result == null) {
            return IntentionPreviewInfo.EMPTY;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethodCallExpression.class);

        if (methodCallExpression != null) {
            PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
            String sourceArgsName = expressions[0].getText();
            BeanUtilHelper.Result invoke = BeanUtilHelper.invoke(methodCallExpression);
            PsiClass targetClass = invoke.targetClass();
            Set<String> commonPropertyNames = BeanUtilHelper.findCommonPropertyNames(invoke);
            List<String> ignoreProperties = invoke.ignoredProperties();
            if (!ignoreProperties.isEmpty()) {
                ignoreProperties.forEach(commonPropertyNames::remove);
            }

            String codeText = targetClass.getName() + " target = new " + targetClass.getName() + "();\n";

            String collect = commonPropertyNames.stream().map(
                    propertyName -> {
                        // 将属性名的首字母大写
                        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                        String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                        return "target." + setterName + "(" + sourceArgsName + "." + getterName + "()" + ");";
                    }
            ).collect(Collectors.joining("\n"));

            String commentWithIndent = codeText + collect + '\n';
            return new IntentionPreviewInfo.Html(commentWithIndent);
        }

        return IntentionPreviewInfo.EMPTY;

    }
}
