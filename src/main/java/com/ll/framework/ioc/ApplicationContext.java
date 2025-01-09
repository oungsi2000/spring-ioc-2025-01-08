package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Component;
import javassist.tools.reflect.CannotCreateException;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ApplicationContext {
    private final Map<String, Object> context = new HashMap<>();
    private final String basePackage;
    private Set<Class<?>> annotatedClassesAll;
    private Set<Method> annotatedMethodsAll;

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflectionsAll = new Reflections(basePackage, Scanners.TypesAnnotated, Scanners.MethodsAnnotated, Scanners.MethodsReturn); // your.root.package 하위의 모든 패키지 스캔
        this.annotatedClassesAll = reflectionsAll.getTypesAnnotatedWith(Component.class);
        this.annotatedMethodsAll = reflectionsAll.getMethodsAnnotatedWith(Bean.class);
        genAll();
    }

    private <T> T genBeanByInvocation(String beanName) throws Exception {
        // your.root.package 하위의 모든 패키지 스캔
        List<Method> files;
        files = annotatedMethodsAll.stream()
            .filter(name -> name.getName().contains(beanName))
            .toList();
        if (files.isEmpty()) {
            Reflections reflectionsAll = new Reflections(basePackage, Scanners.MethodsReturn);
            files = reflectionsAll.getMethodsReturn(Class.forName(beanName)).stream().toList();
            if (files.isEmpty()) throw new CannotCreateException("bean " + beanName + " 를 찾을 수 없습니다");
        }

        List<T> instancedParameters = new ArrayList<>();
        for (Parameter parameter : files.getFirst().getParameters()) {
            T instance = genBean(parameter.toString().split(" ")[0]);
            instancedParameters.add(instance);
        }

        Class<?> targetClass = files.getFirst().getDeclaringClass();
        T instance = (T) targetClass.getDeclaredConstructors()[0].newInstance();
        String fullDirName = targetClass.getName()+"."+files.getFirst().getName();
        T beanInstance = (T) files.getFirst().invoke(instance, instancedParameters.toArray());
        context.put(fullDirName, beanInstance);
        return (T) beanInstance;
    }

    private <T> T getBean(String beanName) {
        String UpperBeanName =  beanName.substring(0,1).toUpperCase() + beanName.substring(1);
        for (String key : context.keySet())  {
           if (key.contains(beanName) || key.contains(UpperBeanName)) return (T) context.get(key);
       }
       return null;
    }

    /**
     * @param beanName
     * @return T
     * @apiNote 빈 생성 순서 : 객체 > 의존 객체 > 빈 객체 > 의존 빈 객체 순서입니다
     * 내부적으로 빈 이름 저장 방법 : 객체 : package + className, 빈 메서드 package + className + methodName
     * 모든 className은 대문자로 시작하며, 모든 methodname은 소문자로 시작합니다
     * 만약 객체를 소문자로 찾으려 한다면 앞의 첫 글자를 자동으로 대문자로 반환합니다
     */
    public <T> T genBean(String beanName) {
        if (getBean(beanName) != null) { return getBean(beanName); }
        String UpperBeanName =  beanName.substring(0,1).toUpperCase() + beanName.substring(1);
        try {
            List<String> files;
            //TODO beanName 과 일치하는 경로를 동적으로 찾는 부분을 메서드 분리
            files = annotatedClassesAll.stream()
                .map(Class::getName)
                .filter(name -> name.contains(UpperBeanName) || name.contains(beanName))
                .toList();

            //만약 의존 객체를 프로젝트 디렉토리에서 찾을 수 없다면 BaseCreateExcpetion 처리
            if (files.isEmpty())  {
                return genBeanByInvocation(beanName);
            }
            //해당 이름과 일치하는 첫 번째 클래스 생성
            Class<T> bean = (Class<T>) Class.forName(files.getFirst());
            Constructor<T> constructor = (Constructor<T>) bean.getConstructors()[0];

            Class[] parameters = constructor.getParameterTypes();
            List<T> instancedParameters = new ArrayList<>();

            //재귀적으로 필요한 인자를 동적으로 생성
            for (Class parameter : parameters) {
                T instance = genBean(parameter.getName());
                instancedParameters.add(instance);
            }

            T instance = constructor.newInstance(instancedParameters.toArray());
            context.put(files.getFirst(), instance);
            return instance;

        } catch (CannotCreateException e){
            //TODO 만약 빈을 생성할 수 없다면 따로 후처리
            return (T) null;
        } catch (Exception e) {
            return (T) null;
        }
    }

    private <T> void genAll() {
        annotatedClassesAll.forEach(
            annotatedClass-> genBean(annotatedClass.getName())
            );
        annotatedMethodsAll.forEach(
            annotatedMethod->genBean(annotatedMethod.getName())
        );
    }
}