package silencecorner.intellij.plugin.evo;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.Nullable;

public class GenerateGenerateEnumValueOfActionHandler extends EditorWriteActionHandler {
    @Override
    public void executeWriteAction(final Editor editor, @Nullable Caret caret, final DataContext dataContext) {

        PsiHelper util = ApplicationManager.getApplication().getComponent(PsiHelper.class);
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(editor.getProject());
        PsiElementFactory psiElementFactory = psiFacade.getElementFactory();

        PsiClass clazz = util.getCurrentClass(editor);
        PsiParameter psiParameter = clazz.getConstructors()[0].getParameterList().getParameters()[0];
        String compareText;
        if (TypeConversionUtil.getTypeRank(psiParameter.getType()) <= TypeConversionUtil.INT_RANK) {
            compareText = "== %s";
        }else{
            if (psiParameter.getType().getPresentableText()
                    .equals(psiParameter.getType().getPresentableText().toLowerCase())){
                Messages.showMessageDialog(editor.getProject(),
                        "constructor parameter type long float double must be Long Float Double",
                        "type error ",
                        Messages.getWarningIcon());
                return;
            }
            compareText = ".equal(%s)";
        }
        String strGetMthod = "get" + psiParameter.getName().substring(0,1).toUpperCase() + psiParameter.getName().substring(1);
        if (clazz.findMethodsByName(strGetMthod,false).length < 1){
            Messages.showMessageDialog(editor.getProject(),
                    "use this plugin must have  a public get method!",
                    String.format("the method %s not exist",strGetMthod),
                    Messages.getWarningIcon());
            return;
        }

        //删除已存在valueOf方法
        PsiMethod[] psiMethods = clazz.findMethodsByName("valueOf",false);
        for (PsiMethod method : psiMethods){
            method.delete();
        }

        PsiMethod psiMethod = psiElementFactory.createMethodFromText("public static " + clazz.getName() +" valueOf(" + psiParameter.getType().getPresentableText() +  " " + psiParameter.getName() +") {}",null);

        //创建foreach语句
        String text = "for("+ clazz.getName() + " " + clazz.getName().toLowerCase() + ":" + clazz.getName() + ".values()){"
                    + "if (" + clazz.getName().toLowerCase() + "." + strGetMthod + "()" + String.format(compareText,psiParameter.getName())  +"){return " + clazz.getName().toLowerCase() +";}}";
        psiMethod.getBody().add(psiElementFactory.createStatementFromText(text, null));
        psiMethod.getBody().add(psiElementFactory.createStatementFromText("return null;", null));
        CodeStyleManager styleManager = CodeStyleManager.getInstance(editor.getProject());
        PsiElement psiElement = styleManager.reformat(psiMethod);

        clazz.add(psiElement);
    }
}