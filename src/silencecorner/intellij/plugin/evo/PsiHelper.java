package silencecorner.intellij.plugin.evo;


import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"ConstantConditions"})
public class PsiHelper implements ApplicationComponent {

    public PsiHelper() {
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @Override
    @NotNull
    public String getComponentName() {
        return getClass().getSimpleName();
    }



    public PsiClass getCurrentClass(final Editor editor) {
        if (editor == null) {
            return null;
        }
        PsiManager psiManager = PsiManager.getInstance(editor.getProject());
        VirtualFile vFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        PsiFile psiFile = psiManager.findFile(vFile);
        if (!(psiFile instanceof PsiJavaFile)) {
            return null;
        }
        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiElement element = javaFile.findElementAt(editor.getCaretModel().getOffset());
        while (!(element instanceof PsiClass) && element != null) {
            element = element.getParent();
        }
        if (element == null) {
            return null;
        } else {
            return (PsiClass) element;
        }
    }
}
