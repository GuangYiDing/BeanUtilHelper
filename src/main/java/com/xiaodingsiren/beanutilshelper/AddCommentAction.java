package com.xiaodingsiren.beanutilshelper;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rose Ding
 * @since 2024/1/30 10:05
 */
public class AddCommentAction implements IntentionAction {


    @Override
    public @IntentionName @NotNull String getText() {
        return "BeanUtilHelper - Show copy properties";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return this.getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
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
        if (!canonicalText.startsWith("BeanUtil.copyProperties") && !canonicalText.startsWith("BeanUtils.copyProperties")) {
            return false;
        }


        PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
        if (expressions.length < 2) {
            return false;
        }

        try {
            // 假设获取字段名称的过程没有错误，此处省略编写获取字段和类型的详细方法
            PsiClass sourceClass = PsiUtil.resolveClassInType(expressions[0].getType());
            PsiClass targetClass = PsiUtil.resolveClassInType(expressions[1].getType());

            // 处理 Class<T> tClass 参数
            if (expressions[1] instanceof PsiClassObjectAccessExpression){
                targetClass = PsiTypesUtil.getPsiClass(((PsiTypeElementImpl) expressions[1].getFirstChild()).getType());
            }

            if (sourceClass == null || targetClass == null) {
                return false;
            }

            // 获取并比较两个类的字段，这里省略实际获取字段的逻辑
            Set<String> commonPropertyNames = findCommonPropertyNames(sourceClass, targetClass);

            // 处理 ignoreProperties 参数
            List<String> ignoreProperties = getIgnoreProperties(expressions);
            if (expressions.length >2 && ignoreProperties.size() > 0) {
                ignoreProperties.forEach(commonPropertyNames::remove);
            }

            if (commonPropertyNames.isEmpty()) {
                return false;
            }

        } catch (Exception e) {
            // 合适的异常处理
            return false;
        }

        return true;
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
                PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
                PsiClass sourceClass = PsiUtil.resolveClassInType(expressions[0].getType());
                PsiClass targetClass = PsiUtil.resolveClassInType(expressions[1].getType());

                // 处理 Class<T> tClass 参数
                if (expressions[1] instanceof PsiClassObjectAccessExpression){
                    targetClass = PsiTypesUtil.getPsiClass(((PsiTypeElementImpl) expressions[1].getFirstChild()).getType());
                }

                if (sourceClass == null || targetClass == null) {
                    return;
                }
                Set<String> commonPropertyNames = findCommonPropertyNames(sourceClass, targetClass);

                // 处理 ignoreProperties 参数
                List<String> ignoreProperties = getIgnoreProperties(expressions);
                if (ignoreProperties.size() > 0) {
                    ignoreProperties.forEach(commonPropertyNames::remove);
                }

                // 计算要插入注释的位置
                int lineNum = document.getLineNumber(methodCallExpression.getTextRange().getStartOffset());
                int lineStartOffset = document.getLineStartOffset(lineNum);
                // 缩进的位置
                String linePrefix = document.getText(new TextRange(lineStartOffset, methodCallExpression.getTextRange().getStartOffset()));
                String commentText;
                // 共有属性超过四个使用块注释显示
                if (commonPropertyNames.size() > 4) {
                    commentText =  commonPropertyNames.stream()
                            .map(propertyName -> linePrefix + "\t\t" + propertyName)
                            .collect(Collectors.joining(",\n",
                                    String.format("""
                                            /*
                                            %s    从 %s 对象中复制属性:
                                            """, linePrefix,  sourceClass.getName()),
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

    private List<String> getIgnoreProperties( PsiExpression[]  expressions) {
        if (expressions.length > 2 ){
            return Stream.of(expressions)
                    .filter(expression -> expression instanceof PsiLiteralExpression)
                    .map(e -> e.getText().replace("\"","")).collect(Collectors.toList());
        }
        return List.of();
    }


    @Override
    public boolean startInWriteAction() {
        return false;
    }

}
