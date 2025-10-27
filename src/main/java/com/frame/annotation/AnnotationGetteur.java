package com.frame.annotation;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class AnnotationGetteur {

    private String packageName ;

    public AnnotationGetteur(String packageName){
        this.packageName=packageName;
    }

    //scan tous les classes dans un file
    private static void scanDirectory(File directory , String packageName , List<Class<?>> classList) throws Exception{
        File [] files = directory.listFiles();
        if (files==null) return ;

        for(File file :files ){
            if(file.isDirectory()){
                scanDirectory(file, packageName+"."+file.getName(), classList);
            } else if (file.getName().endsWith(".class")){
                String nomClass = packageName+"."+file.getName().substring(0 , file.getName().length() - 6) ;               
                try {
                    Class<?> clazz = Class.forName(nomClass);
                    classList.add(clazz);
                } catch (Exception e) {
                    System.err.println("La class "+nomClass+" ne peut pas etre charger");
                }
            }
        }
    }

    //scan tous les classes dans un file avec une annotation precise
    private static <T extends Annotation> void scanDirectoryWithAnnotation(File directory , String packageName , List<Class<?>> classList , Class<T> annonationClass) throws Exception{
        File [] files = directory.listFiles();
        if (files==null) return ;

        for(File file :files ){
            if(file.isDirectory()){
                scanDirectory(file, packageName+"."+file.getName(), classList);
            } else if (file.getName().endsWith(".class")){
                String nomClass = packageName+"."+file.getName().substring(0 , file.getName().length() - 6) ;               
                try {
                    Class<?> clazz = Class.forName(nomClass);
                    if(clazz.isAnnotationPresent(annonationClass)){
                        classList.add(clazz);
                    }
                } catch (Exception e) {
                    System.err.println("La class "+nomClass+" ne peut pas etre charger");
                }
            }
        }
    }

    //get all classes dans un package et ces fichier interne 
    private static List<Class<?>> getAllClasses(String packageProject) throws Exception{
        List<Class<?>> classes = new ArrayList<>();

        String path = packageProject.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new Exception("Package non trouvé: " + packageProject);
        }
        File directory = new File(resource.getFile());
        if (!directory.exists()) {
            throw new Exception("Répertoire non trouvé: " + directory.getAbsolutePath());
        }
        scanDirectory(directory, packageProject, classes);

        return classes;
    }

    //get all classes dans un package et ces fichier interne avec une annotation precise
    private static <T extends Annotation> List<Class<?>> getAllClassesWithAnnotation(String packageProject , Class<T> annotationClass) throws Exception{
        List<Class<?>> classes = new ArrayList<>();

        String path = packageProject.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new Exception("Package non trouvé: " + packageProject);
        }
        File directory = new File(resource.getFile());
        if (!directory.exists()) {
            throw new Exception("Répertoire non trouvé: " + directory.getAbsolutePath());
        }
        scanDirectoryWithAnnotation(directory, packageProject, classes , annotationClass);

        return classes;
    }

    //get liste des methodes dans une package
    private static List<Method> getAllMethods(String packageProject) throws Exception{
        List<Method> methods = new ArrayList<>();

        for (Class<?> class1 : getAllClasses(packageProject)) {
            for (Method method : class1.getDeclaredMethods()){
                methods.add(method);
            }
        }

        return methods;
    }

    //get liste des methodes dans une package avec annotation de class precise
    private static <T extends Annotation> List<Method> getAllMethodsWithAnnotation(String packageProject, Class<T> annotationClass) throws Exception{
        List<Method> methods = new ArrayList<>();

        for (Class<?> class1 : getAllClassesWithAnnotation(packageProject,annotationClass)) {
            for (Method method : class1.getDeclaredMethods()){
                methods.add(method);
            }
        }

        return methods;
    }

    public static <T extends Annotation> List<T> getAllAnnotationInMethod(String packageProject, Class<T> annotationClass) throws Exception {
        List<T> annotations = new ArrayList<>();
        for(Method method : getAllMethods(packageProject)){
            if (method.isAnnotationPresent(annotationClass)) {
                annotations.add(method.getAnnotation(annotationClass));
            }
        }

        return annotations;
    }

    public static <T extends Annotation> List<T> getAllAnnotationInMethodWithAnnotation(String packageProject, Class<T> annotationClass , Class<T> classAnnotationClass) throws Exception {
        List<T> annotations = new ArrayList<>();
        for(Method method : getAllMethodsWithAnnotation(packageProject,classAnnotationClass)){
            if (method.isAnnotationPresent(annotationClass)) {
                annotations.add(method.getAnnotation(annotationClass));
            }
        }

        return annotations;
    }

    public static List<GetMapping> getAllGetMapping(String packageProject)throws Exception{
        List<GetMapping> get = new ArrayList<>();
        for (Method method : getAllMethodsWithAnnotation(packageProject,Controller.class)) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                get.add(method.getAnnotation(GetMapping.class));
            }
        }

        return get;
    }

    public static List<PostMapping> getAllPostMapping(String packageProject)throws Exception{
        List<PostMapping> post = new ArrayList<>();
        for (Method method : getAllMethodsWithAnnotation(packageProject,Controller.class)) {
            if (method.isAnnotationPresent(PostMapping.class)) {
                post.add(method.getAnnotation(PostMapping.class));
            }
        }

        return post;
    }

    public static List<PutMapping> getAllPutMapping(String packageProject)throws Exception{
        List<PutMapping> put = new ArrayList<>();
        for (Method method : getAllMethodsWithAnnotation(packageProject,Controller.class)) {
            if (method.isAnnotationPresent(PutMapping.class)) {
                put.add(method.getAnnotation(PutMapping.class));
            }
        }

        return put;
    }

    public static List<DeleteMapping> getAllDeleteMapping(String packageProject)throws Exception{
        List<DeleteMapping> delete = new ArrayList<>();
        for (Method method : getAllMethodsWithAnnotation(packageProject,Controller.class)) {
            if (method.isAnnotationPresent(DeleteMapping.class)) {
                delete.add(method.getAnnotation(DeleteMapping.class));
            }
        }

        return delete;
    }



}
