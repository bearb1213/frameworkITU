package com.frame.model;

import java.lang.reflect.Method;

public class Mapping {

    private Class<?> clazz;
    private Method method;
    private String path;
    private String annotation;
    

    public Mapping(Class<?> clazz , Method method , String path , String annotation){
        this.clazz=clazz;
        this.method=method;
        this.path=path;
        this.annotation=annotation;
    }

    public Class<?> getClazz(){
        return clazz;
    }
    public void setClazz(Class<?> clazz){
        this.clazz=clazz;
    }
    public Method getMethod(){
        return method;   
    }
    public void setMethod(Method method){
        this.method=method;
    }
    public String getPath(){
        return path;
    }
    public void setPath(String path){
        this.path = path;
    }
    public void setAnnotation(String annotation){
        this.annotation=annotation;
    }
    public String getAnnotation(){
        return annotation;
    }

}
