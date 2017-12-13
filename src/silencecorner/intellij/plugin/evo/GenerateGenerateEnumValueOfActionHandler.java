package silencecorner.intellij.plugin.evo;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
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
        if (!check(psiParameter.getType())){
            showMsg(editor);
            return;
        }
        //删除已存在valueOf方法
        PsiMethod[] psiMethods = clazz.findMethodsByName("valueOf",false);
        for (PsiMethod method : psiMethods){
            method.delete();
        }
        PsiMethod psiMethod = psiElementFactory.createMethodFromText("public static " + clazz.getName() +" valueOf(" + psiParameter.getType().getPresentableText() +  " " + psiParameter.getName() +") {}",null);
        //创建switch case语句
        String text = "switch (" + psiParameter.getName() + ") {}";
        PsiSwitchStatement switchBlock = (PsiSwitchStatement)psiElementFactory.createStatementFromText(text, clazz);
        int i = 0;
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
        clazz.add(psiMethod);
    }
    public String getCase(String text){
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            return text.replace(m.group(0),"");
        }
        return text;
    }
    private enum SelectorKind {
        INT, ENUM, STRING
    }

    private static SelectorKind getSwitchSelectorKind(@NotNull PsiType type) {
        if (TypeConversionUtil.getTypeRank(type) <= TypeConversionUtil.INT_RANK) {
            return SelectorKind.INT;//这里指定的是整形
        }

        PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(type);
        if (psiClass != null) {
            if (psiClass.isEnum()) {
                return SelectorKind.ENUM;
            }
            if (Comparing.strEqual(psiClass.getQualifiedName(), CommonClassNames.JAVA_LANG_STRING)) {
                return SelectorKind.STRING;
            }
        }

        return null;
    }
    private boolean check(PsiType psiType){
        boolean flag = false;
        for (SelectorKind kind : SelectorKind.values()){
            if (kind.equals(getSwitchSelectorKind(psiType))){
                flag = true;
                break;
            }
        }
        return flag;
    }
    private void  showMsg(Editor editor){
        Messages.showMessageDialog(editor.getProject(),
                "the first of enum constructor parameter can't be used for switch", "occur an error",
                Messages.getWarningIcon());
    }
}