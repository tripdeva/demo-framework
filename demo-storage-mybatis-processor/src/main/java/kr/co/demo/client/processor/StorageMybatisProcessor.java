package kr.co.demo.client.processor;

import com.google.auto.service.AutoService;
import kr.co.demo.client.processor.generator.BaseMapperGenerator;
import kr.co.demo.core.storage.annotation.StorageTable;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

/**
 * {@link StorageTable} 어노테이션을 처리하여 MyBatis Mapper를 생성하는 Annotation Processor
 *
 * <p>컴파일 타임에 도메인 객체를 분석하여 기본 CRUD 메서드가 포함된
 * MyBatis Mapper 인터페이스를 자동 생성합니다.
 *
 * <p>생성되는 파일:
 * <ul>
 *     <li>{@code {도메인명}BaseMapper.java} - 기본 CRUD Mapper 인터페이스</li>
 * </ul>
 *
 * <p>생성된 Mapper를 상속하여 커스텀 메서드를 추가할 수 있습니다:
 * <pre>{@code
 * @Mapper
 * public interface OrderMapper extends OrderBaseMapper {
 *     // 커스텀 메서드 추가
 *     List<Order> findByStatus(OrderStatus status);
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 * @see StorageTable
 * @see BaseMapperGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("kr.co.demo.core.storage.annotation.StorageTable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class StorageMybatisProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private BaseMapperGenerator baseMapperGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.baseMapperGenerator = new BaseMapperGenerator(filer);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(StorageTable.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error("@StorageTable은 클래스에만 사용 가능합니다", element);
                continue;
            }

            TypeElement domainClass = (TypeElement) element;
            String packageName = getPackageName(domainClass);

            try {
                baseMapperGenerator.generate(domainClass, packageName);
                note("Generated MyBatis BaseMapper for " + domainClass.getSimpleName());
            } catch (IOException e) {
                error("코드 생성 실패: " + e.getMessage(), element);
            }
        }

        return true;
    }

    private String getPackageName(TypeElement element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void note(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }
}
