package com.frame.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.rowset.serial.SerialException;

import com.frame.annotation.AnnotationGetteur;
import com.frame.annotation.RequestParam;
import com.frame.model.Mapping;
import com.frame.model.ModelView;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class FrontServlet extends HttpServlet {

    private Map<String , Mapping > getMappings;
    private Map<String , Mapping > postMappings;

    private String packageName;
 
    @Override
    public void init() throws ServletException{
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = getServletContext()
        .getResourceAsStream("/WEB-INF/frame.properties");
            prop.load(input);
            packageName = prop.getProperty("package.name");
            if (packageName==null || packageName.isEmpty()) {
                packageName = "";
            }

            getMappings=AnnotationGetteur.getAllGetMappings(packageName);
            postMappings=AnnotationGetteur.getAllPostMappings(packageName);
        } catch (Exception e) {
            System.out.println("Fichier de configuration non present");
        } finally {
            if(input!= null) {
                try {
                    input.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
            
    }

    private void work(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        
        String method = request.getMethod();  
        String url = request.getRequestURI();
        
        // prendre le path de base de l'app
        ServletContext context = getServletContext();
        String appPath = context.getRealPath("/");

        // separe le nom de lapp avec le 
        int firstSlash = url.indexOf('/', 1); 
        String resourcePath = url.substring(firstSlash+1);

        File file = new File(appPath,resourcePath);
        if (file.exists()) {
            if( file.isFile()){
                response.setContentType(Files.probeContentType(file.toPath()));

                response.setCharacterEncoding("UTF-8");

                try (InputStream in = new FileInputStream(file);
                    OutputStream out = response.getOutputStream()) {
                    
                    byte[] buffer = new byte[4096]; // 4 Ko
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                    return ;
                }
            }
            
        } else {
            String methodeHttp = request.getMethod();
            resourcePath = "/"+resourcePath;
            Mapping mapping ;
            Map<String , Mapping> mappings;
            switch (methodeHttp) {
                case "GET":
                    mapping=getMappings.get(resourcePath);
                    mappings=getMappings;
                    break;
                case "POST" :
                    mapping=postMappings.get(resourcePath);
                    mappings=postMappings;
                    break;
                default:
                    mapping=getMappings.get(resourcePath);
                    mappings=getMappings;
                    break;
            }
            if (mapping==null) {
                for (Entry<String , Mapping> map : mappings.entrySet()) {
                    String regex = map.getKey().replaceAll("\\{([^}]+)\\}", "([^/]+)");
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcherPath = pattern.matcher(resourcePath);
                    if (matcherPath.matches()) {
                        Map<String , String> params= new HashMap<>();
                         // Créer un pattern pour extraire les noms des paramètres
                        Pattern keyPattern = Pattern.compile("\\{([^}]+)\\}");
                        Matcher keyMatcher = keyPattern.matcher(map.getKey());
                        int groupIndex = 1;
                        while (keyMatcher.find() && groupIndex <= matcherPath.groupCount()) {
                            String paramName = keyMatcher.group(1);
                            String paramValue = matcherPath.group(groupIndex);
                            params.put(paramName, paramValue);
                            groupIndex++;
                        }
                        traite(request, response, map.getValue(), params);
                        return;

                    }
                }    
                throw new ServletException("404 not found");
                
            }else {
                traite(request, response, mapping , null);
                return;
            }
            
        }
        
    }

    private void traite(HttpServletRequest request, HttpServletResponse response,Mapping mapping , Map<String , String> arguments)
            throws ServletException, IOException {
        
        try {
            // creation de l'instance
            Object instance = mapping.getClazz().getDeclaredConstructor().newInstance();
            // recupertation de la methode et ces parametres
            Method method = mapping.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] parameterToAssign = new Object[parameters.length];
            // assignation des parametre
            for (int i = 0; i < parameters.length; i++) {
                String object = null;
                boolean isPathVariable = false; 

                // getteur des valeur des params
                if (arguments!=null && !arguments.isEmpty()) {
                    object = arguments.get(parameters[i].getName());
                    isPathVariable = (object!=null);
                    
                } 
                if (!isPathVariable) {                    
                    if (parameters[i].isAnnotationPresent(RequestParam.class) ) {
                        RequestParam Rparam = parameters[i].getAnnotation(RequestParam.class);
                        object = request.getParameter(Rparam.value());
                        
                    } else {
                        object = request.getParameter(parameters[i].getName());
                    }
                }

                //caste des objet au attribut
                if (parameters[i].getType().equals(int.class) || parameters[i].getType().equals(Integer.class)) {
                    
                    if (object!= null) parameterToAssign[i] = Integer.parseInt(object);
                    else parameterToAssign[i] = 0;
                
                } else if (parameters[i].getType().equals(double.class) || parameters[i].getType().equals(Double.class)) {
                
                    if (object!= null) parameterToAssign[i] = Double.parseDouble(object);
                    else parameterToAssign[i]=0.;
                
                } else if (parameters[i].getType().equals(float.class) || parameters[i].getType().equals(Float.class)) {
                
                    if (object!=null) parameterToAssign[i] = Float.parseFloat(object);
                    else parameterToAssign[i] = 0.;
                
                } else if (parameters[i].getType().equals(String.class)) {
                
                    parameterToAssign[i] = object;
                
                } else if (parameters[i].getType().isAssignableFrom(Map.class)){
                    
                    if (isMapStringObject(parameters[i])){
                        Enumeration<String> parameterNames = request.getParameterNames();
                        Map<String , Object> map = new HashMap<>();
                        while (parameterNames.hasMoreElements()) {
                            String paramName = parameterNames.nextElement();
                            Object paramValue = cast(request.getParameter(paramName));
                            map.put(paramName, paramValue);
                        }
                        parameterToAssign[i] = map;
                    } else {
                        throw new Exception("Le map doit etre de type Map<String ,Object>");
                    }
                }
             
            }
            // invocation de la methode

            Object retour = method.invoke(instance, parameterToAssign);
            if (retour.getClass().equals(ModelView.class)) {
                ModelView mv = (ModelView) retour;
                Map<String ,Object> attributs = mv.getAttributs();
                if (attributs != null && !attributs.isEmpty()) {   
                    for (Entry<String , Object> attribut : attributs.entrySet()) {
                        request.setAttribute(attribut.getKey(),attribut.getValue());
                    }
                }
                request.getRequestDispatcher(mv.getView()).forward(request, response);
                return;
                // si le retour est string 
            } else if (retour.getClass().equals(String.class)){
                String str = (String)retour;
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                // soloina forward rehefa tenenina
                out.print(str);
                return;
            } else {
                throw new ServletException("Type de retour invalide : "+retour.getClass().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        } 
    
    }



    private void print(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            
            
        PrintWriter out = response.getWriter();
        try {
            out.println("package name : "+packageName);
            
        } catch (Exception e) {
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            out.println(e.getMessage());
        }


    }

    private Object cast(Object value){
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

            return (String) value;
        }
        return null;
    }

    private boolean isMapStringObject(Parameter p) throws Exception{
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

 
    





    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        

        try {
            work(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            // PrintWriter out = response.getWriter();
            // out.print(e);
            throw new ServletException(e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        

        try {
            
            work(request, response);
        } catch (Exception e) {

            // e.printStackTrace();
            // PrintWriter out = response.getWriter();
            // out.print(e);
            throw new ServletException(e);
        }
    }
}