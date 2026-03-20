package kr.co.demo.client.processor.util;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;
import kr.co.demo.core.storage.annotation.Patch;

// =======================
// Annotation Processor (@Patch 처리)
// =======================
@AutoService(Processor.class)
@SupportedAnnotationTypes("kr.co.demo.patch.annotation.Patch")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class PatchMethodProcessor extends AbstractProcessor {

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(Patch.class)) {

            if (element.getKind() != ElementKind.METHOD) {
                error("@Patch 는 메서드에만 사용 가능합니다", element);
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;

            // 파라미터 1개 강제
            if (method.getParameters().size() != 1) {
                error("@Patch 메서드는 파라미터 1개만 허용", method);
            }

            // 반환 타입 int 강제
            if (!method.getReturnType().toString().equals("int")) {
                error("@Patch 메서드는 int 반환만 허용", method);
            }

            // 실제 Mapper 생성은 BaseMapperGenerator에서 수행
            // 여기서는 검증만 담당
        }

        return true;
    }

    private void error(String msg, Element e) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
}

