package silencecorner.intellij.plugin.evo;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateGenerateEnumValueOfActionHandler extends EditorWriteActionHandler {
    private static final Pattern pattern = Pattern.compile("\\(.*\\)");
    @Override
    public void executeWriteAction(final Editor editor, @Nullable Caret caret, final DataContext dataContext) {

        PsiHelper util = ApplicationManager.getApplication().getComponent(PsiHelper.class);
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(editor.getProject());
        PsiElementFactory psiElementFactory = psiFacade.getElementFactory();
        PsiClass clazz = util.getCurrentClass(editor);
        PsiParameter psiParameter = clazz.getConstructors()[0].getParameterList().getParameters()[0];
        PsiMethod psiMethod = psiElementFactory.createMethodFromText("public static " + clazz.getName() +" valueOf(" + psiParameter.getType().getPresentableText() +  " " + psiParameter.getName() +") {}",null);
        //创建switch case语句
        String text = "switch (" + psiParameter.getName() + ") {}";
        PsiSwitchStatement switchBlock = (PsiSwitchStatement)psiElementFactory.createStatementFromText(text, clazz);
        //不能使用switch分支的类型弹出详细框
        if (switchBlock.isValid()){
            Messages.showMessageDialog(editor.getProject(),
                    "the first of enum constructor parameter can't be used by switch", "occur an error",
                    Messages.getErrorIcon());
            return;
        }
        for (PsiField field : clazz.getFields()) {
            if (field instanceof PsiEnumConstant) {
                String fieldName = field.getName();
                PsiExpression[] psiExpressions =  ((PsiEnumConstant) field).getArgumentList().getExpressions();
                switchBlock.getBody().add(psiElementFactory.createStatementFromText(
                        "case " + getCase(psiExpressions[0].getText()) + ":"
                        ,
                        null));
                switchBlock.getBody().add(psiElementFactory.createStatementFromText(
                        "return " +  fieldName + ";",
                        null));

            }

        }
        switchBlock.getBody().add(psiElementFactory.createStatementFromText("default:", switchBlock));
        switchBlock.getBody().add(psiElementFactory.createStatementFromText("throw new RuntimeException(\"invalid  " + psiParameter.getName() + " for enum " + clazz.getName() + "\");",switchBlock));
        CodeStyleManager styleManager = CodeStyleManager.getInstance(editor.getProject());
        PsiElement psiElement = styleManager.reformat(switchBlock);
        psiMethod.getBody().add(psiElement);
        if (psiMethod.isValid()){
            Messages.showMessageDialog(editor.getProject(),
                    "method invalid:\n" + psiMethod.getText(), "occur an error",
                    Messages.getErrorIcon());
            return;
        }
        clazz.add(psiMethod);
    }
    public String getCase(String text){
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            return text.replace(m.group(0),"");
        }
        return text;
    }
}