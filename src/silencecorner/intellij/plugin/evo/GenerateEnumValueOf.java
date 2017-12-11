package silencecorner.intellij.plugin.evo;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.psi.PsiClass;


public class GenerateEnumValueOf extends EditorAction {

    public GenerateEnumValueOf() {
        super(new GenerateGenerateEnumValueOfActionHandler());
    }

    protected GenerateEnumValueOf(EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    @Override
    public void update(Editor editor, Presentation presentation, DataContext dataContext) {
        PsiHelper util = ApplicationManager.getApplication()
                .getComponent(PsiHelper.class);
        if (editor == null) {
            presentation.setVisible(false);
            return;
        }
        PsiClass javaClass = util.getCurrentClass(editor);
        //设置enum可访问,且只有一个构造函数,且构造有至少一个参数
        if (javaClass != null
                && javaClass.isEnum()
                && javaClass.getConstructors() != null
                && javaClass.getConstructors().length == 1
                && javaClass.getConstructors()[0].getParameterList().getParameters().length > 0) {
            presentation.setVisible(true);
            return;
        }
        presentation.setVisible(false);
    }


}
