package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Configuration;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes({"com.ll.framework.ioc.annotations.Configuration"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class IocAnnotationProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment env){ }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing round: " + roundEnv.processingOver()); // 현재 라운드 정보 출력

        for (Element element : roundEnv.getElementsAnnotatedWith(Configuration.class)) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                String className = typeElement.getQualifiedName().toString();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found class with @MyAnnotation: " + className);
            }
        }

        if (roundEnv.processingOver()) { // 마지막 라운드인 경우 메시지 출력
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Annotation processing finished.");
        }
        return true;
    }
}