package com.frame.util;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.frame.model.ModelView;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utilitaire {
    public static boolean isMapStringObject(Parameter p) throws Exception{
        Type type = p.getParameterizedType();

        ParameterizedType pType = (ParameterizedType) type;

        if (pType.getRawType() != Map.class) {
            return false;
        }
        
        Type[] typeArgs = pType.getActualTypeArguments();
        
        // Vérifier Map<String, Object>
        return typeArgs.length == 2 && 
               typeArgs[0] == String.class && 
               typeArgs[1] == Object.class;
    }

    public static Object cast(Object value){
        if (value==null) return null;
        if(value.getClass().isAssignableFrom(String.class)){
            String val = (String) (value);
            if (val.isEmpty()) return "";
            try {
                Integer i = Integer.parseInt(val);
                return i;
            } catch (Exception e) {
            }
            try {
                Double d = Double.parseDouble(val);
                return d;
            } catch (Exception e) {
            }
            try {
                Float f = Float.parseFloat(val);
                return f;
            } catch (Exception e) {
            }

            return (String) value;
        }
        return null;
    }

    public static Object cast(Object value,Class<?> clazz){
        if (value==null) return null;
        if(value.getClass().isAssignableFrom(String.class)){
            String val = (String) (value);
            if(clazz.equals(Integer.class) || clazz.equals(int.class)){
                return Integer.parseInt(val);
            } 
            if (clazz.equals(Float.class) || clazz.equals(float.class)) {
                return Float.parseFloat(val);
            }
            if (clazz.equals(Double.class) || clazz.equals(double.class)){
                return Double.parseDouble(val);
            }
            return val;
        }
        return null;
    }

    public static boolean isTypeGenerique(Class<?> clazz){
        return (clazz.equals(Integer.class) || clazz.equals(int.class) || clazz.equals(Float.class) || clazz.equals(float.class) || clazz.equals(Double.class) || clazz.equals(double.class) || clazz.equals(String.class) );
    }
    public static boolean isTypeGeneriqueArray(Class<?> clazz){
        return (clazz.isArray() && (clazz.getComponentType().equals(Integer.class) || clazz.getComponentType().equals(int.class) || clazz.getComponentType().equals(Float.class) || clazz.getComponentType().equals(float.class) || clazz.getComponentType().equals(Double.class) || clazz.getComponentType().equals(double.class) || clazz.getComponentType().equals(String.class) ));
    }
    public static boolean isListObjectGenerique(Type type) throws Exception{

        ParameterizedType pType = (ParameterizedType) type;

        if (pType.getRawType() != List.class) {
            return false;
        }
        
        Type[] typeArgs = pType.getActualTypeArguments();
        
        // Vérifier Map<String, Object>
        return typeArgs.length == 1 && 
               (typeArgs[0] == String.class || typeArgs[0] == Integer.class || typeArgs[0] == int.class || typeArgs[0] == Double.class || typeArgs[0] == double.class || typeArgs[0] == Float.class || typeArgs[0] == float.class);
    }
    public static String capitalizeFirst(String str){
        String debut = str.substring(0,1);
        String fin = str.substring(1);
        return debut.toUpperCase()+fin;
    }

    public static String jsonify(Object o){
        if (o instanceof ModelView) {
            return jsonify(((ModelView) o).getAttributes());
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(o);
                // System.out.println(json);
                return json;
            } catch (Exception e) {
                e.printStackTrace();
                return "{}";
            }
        }
        
    }



}
