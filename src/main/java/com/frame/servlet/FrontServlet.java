package com.frame.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Array;
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
import java.util.Collection;

import javax.sql.rowset.serial.SerialException;

import com.frame.annotation.AnnotationGetteur;
import com.frame.annotation.RequestParam;
import com.frame.extension.FileExtension;
import com.frame.annotation.Json;
import com.frame.model.ApiResponse;
import com.frame.model.Mapping;
import com.frame.model.ModelView;
import com.frame.model.Session;
import com.frame.util.Utilitaire;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.servlet.annotation.MultipartConfig;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize = 1024 * 1024 * 10,  
    maxRequestSize = 1024 * 1024 * 50 
)
public class FrontServlet extends HttpServlet {

    private Map<String , Mapping > getMappings;
    private Map<String , Mapping > postMappings;

    private String packageName;
    private String uploadDir ;
 
    @Override
    public void init() throws ServletException{
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = getServletContext()
        .getResourceAsStream("/WEB-INF/frame.properties");
            prop.load(input);
            /// packageName
            packageName = prop.getProperty("package.name");
            if (packageName==null || packageName.isEmpty()) {
                packageName = "";
            }

            /// upload dir
            uploadDir = prop.getProperty("upload.dir");
            if (uploadDir == null || uploadDir.isEmpty()) {
                uploadDir = "uploads";
            }
            if (uploadDir.startsWith("/")) {
                uploadDir = uploadDir.substring(1);
            }
            File uploadDirFile = new File(getServletContext().getRealPath("/"), uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
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
                        System.out.println("\n\n\n\nNIsy Matchhh\n\n\n");
                        traiterRequest(request, response, map.getValue(), params , map.getValue().getMethod().isAnnotationPresent(Json.class));
                        return;

                    }
                }    
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.print("404 Not Found : La ressource "+resourcePath+" n'existe pas");
                return;
                
            }else {
                traiterRequest(request, response, mapping , null , mapping.getMethod().isAnnotationPresent(Json.class));
                return;
            }
            
        }
        
    }

    private void traiterRequest(HttpServletRequest request, HttpServletResponse response,Mapping mapping , Map<String , String> arguments,boolean isJson)
            throws ServletException, IOException {
        
        try {
            // creation de l'instance
            Object instance = mapping.getClazz().getDeclaredConstructor().newInstance();
            // recupertation de la methode et ces parametres
            HttpSession httpSession = null ;
            Session session = null;
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
                // types primitifs 
                // int
                if (parameters[i].getType().equals(int.class) || parameters[i].getType().equals(Integer.class)) {
                    
                    if (object!= null) parameterToAssign[i] = Integer.parseInt(object);
                    else parameterToAssign[i] = 0;
                //double
                } else if (parameters[i].getType().equals(double.class) || parameters[i].getType().equals(Double.class)) {
                
                    if (object!= null) parameterToAssign[i] = Double.parseDouble(object);
                    else parameterToAssign[i]=0.;
                //float
                } else if (parameters[i].getType().equals(float.class) || parameters[i].getType().equals(Float.class)) {
                
                    if (object!=null) parameterToAssign[i] = Float.parseFloat(object);
                    else parameterToAssign[i] = 0.;
                    //string
                } else if (parameters[i].getType().equals(String.class)) {
                    
                    parameterToAssign[i] = object;

                //Session
                } else if (parameters[i].getType().equals(Session.class)) {
                    httpSession = request.getSession();
                    session = new Session(httpSession);

                    parameterToAssign[i] = session;
                //map
                } else if (parameters[i].getType().isAssignableFrom(Map.class)){
                    
                    // map string byte[]
                    if (Utilitaire.isMapStringByteArray(parameters[i])) {
                        Collection<Part> parts = request.getParts();
                        Map<String , byte[]> map = new HashMap<>();
                        for (Part part : parts) {
                            if (part.getSize() > 0) {
                                InputStream inputStream = part.getInputStream();
                                byte[] fileBytes = inputStream.readAllBytes();
                                
                                // Sauvegarde fichier 
                                String uniqueFileName = generateUniqueFileName(part.getSubmittedFileName());
                                File uploadFile = new File(getServletContext().getRealPath("/") + uploadDir, uniqueFileName);
                                part.write(uploadFile.getAbsolutePath());
                                
                                map.put(uniqueFileName, fileBytes);
                            }
                        }
                        parameterToAssign[i] = map;
                    // map FileExtension byte[]
                    } else if (Utilitaire.isMapFileExtensionByteArray(parameters[i])) {
                        Collection<Part> parts = request.getParts();
                        Map<FileExtension , byte[]> map = new HashMap<>();
                        for (Part part : parts) {
                            if (part.getSize() > 0) {
                                InputStream inputStream = part.getInputStream();
                                byte[] fileBytes = inputStream.readAllBytes();
                                
                                // Sauvegarde fichier 
                                String uniqueFileName = generateUniqueFileName(part.getSubmittedFileName());
                                File uploadFile = new File(getServletContext().getRealPath("/") + uploadDir, uniqueFileName);
                                part.write(uploadFile.getAbsolutePath());
                                
                                String originalFileName = part.getSubmittedFileName();
                                String fileExtension = "";
                                int lastDotIndex = originalFileName.lastIndexOf('.');
                                if (lastDotIndex != -1 && lastDotIndex < originalFileName.length() - 1) {
                                    fileExtension = originalFileName.substring(lastDotIndex + 1);
                                }

                                FileExtension fileExt = new FileExtension(uniqueFileName, fileExtension );
                                map.put(fileExt, fileBytes);
                            }
                        }
                        parameterToAssign[i] = map;
                    // map string object
                    } else if (Utilitaire.isMapStringObject(parameters[i])){
                        Enumeration<String> parameterNames = request.getParameterNames();
                        Map<String , Object> map = new HashMap<>();
                        while (parameterNames.hasMoreElements()) {
                            String paramName = parameterNames.nextElement();
                            Object paramValue = Utilitaire.cast(request.getParameter(paramName));
                            map.put(paramName, paramValue);
                        }
                        parameterToAssign[i] = map;
                    } else {
                        throw new Exception("Le map doit etre de type Map<String ,Object>");
                    }
                //liste
                } else if (List.class.isAssignableFrom(parameters[i].getType())) {
                    List<Object> list = (List<Object>) parameters[i].getType().getDeclaredConstructor().newInstance();
                    Object[] paramArray = assignToArray(request, response, (Class<?>) (((ParameterizedType)parameters[i].getParameterizedType()).getActualTypeArguments()[0]), parameters[i].getName());
                    for (Object o : paramArray) {
                        list.add(o);
                    }
                    parameterToAssign[i]= list;
                //array
                }else if(parameters[i].getType().isArray()){
                    Object[] tempArray = assignToArray(request, response, parameters[i].getType().getComponentType(), parameters[i].getName());
                    // Convertir Object[] en array du type correct
                    Class<?> componentType = parameters[i].getType().getComponentType();
                    Object typedArray = Array.newInstance(componentType, tempArray.length);
                    for (int j = 0; j < tempArray.length; j++) {
                        Array.set(typedArray, j, tempArray[j]);
                    }
                    parameterToAssign[i] = typedArray;
                } else if (parameters[i].getType().isAssignableFrom(File.class)){
                    Part filePart = request.getPart(parameters[i].getName());
                    if (filePart != null && filePart.getSize() > 0) {
                        String uniqueFileName = generateUniqueFileName(filePart.getSubmittedFileName());
                        File uploadFile = new File(getServletContext().getRealPath("/") + uploadDir, uniqueFileName);
                        filePart.write(uploadFile.getAbsolutePath());
                        parameterToAssign[i] = uploadFile;
                    } else {
                        parameterToAssign[i] = null;
                    }

                }else {
                    parameterToAssign[i] = assignToObject(request, response, parameters[i].getType(), parameters[i].getName());
                }
             
            }
            // invocation de la methode

            Object retour ;
            try {
                // System.out.println("\n\n\n\nInvocation de la methode "+method.getName()+" de la class "+mapping.getClazz().getName());
                // if (parameterToAssign.length >0)
                // System.out.println("Invocation de parameteres classes : "+parameterToAssign[0].getClass().getComponentType().getName()+"\n\n");

                retour = method.invoke(instance, parameterToAssign);

                if(session!= null && httpSession!=null) {
                    session.merge(httpSession);
                }
                
            } catch (Exception e) {
                if (isJson) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    PrintWriter out = response.getWriter();
                    ApiResponse<?> apiResponse = new ApiResponse<>(500, "Erreur Serveur", e.getMessage());
                    String jsonResponse = Utilitaire.jsonify(apiResponse);
                    out.print(jsonResponse);
                    return;
                } else {
                    throw e;
                }
            }

            if (retour.getClass().equals(ModelView.class)) {
                ModelView mv = (ModelView) retour;
                Map<String ,Object> attributs = mv.getAttributes();
                if (attributs != null && !attributs.isEmpty()) {   
                    for (Entry<String , Object> attribut : attributs.entrySet()) {
                        request.setAttribute(attribut.getKey(),attribut.getValue());
                    }
                } 
                if (isJson) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    PrintWriter out = response.getWriter();
                    ApiResponse<?> apiResponse = new ApiResponse<>(200, "Success", mv.getAttributes());
                    String jsonResponse = Utilitaire.jsonify(apiResponse);
                    out.print(jsonResponse);
                } else {
                    request.getRequestDispatcher(mv.getView()).forward(request, response);
                }
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
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                ApiResponse<?> apiResponse = new ApiResponse<>(200, "Success", retour);
                String jsonResponse = Utilitaire.jsonify(apiResponse);
                out.print(jsonResponse);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isJson) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                ApiResponse<?> apiResponse = new ApiResponse<>(500, "Erreur Serveur", e.getMessage());
                String jsonResponse = Utilitaire.jsonify(apiResponse);
                out.print(jsonResponse);
                return;
            } else {
                throw new ServletException(e);
            }
        } 
    
    }

    

    

    

    private Object assignToObject(HttpServletRequest request, HttpServletResponse response , Class<?> clazz , String prefixe) throws Exception{
        try {
            
            if (clazz.isArray()) {
                return assignToArray(request, response, clazz.getComponentType(), prefixe);
            } 
            if (List.class.isAssignableFrom(clazz)) {
                Object[] paramArray = assignToArray(request, response, Object.class, prefixe);
                List<Object> list = (List<Object>) clazz.getDeclaredConstructor().newInstance();
                for (Object o : paramArray) {
                    list.add(o);
                }
                return list;
            } 
            if(File.class.isAssignableFrom(clazz)){
                Part filePart = request.getPart(prefixe);
                if (filePart != null && filePart.getSize() > 0) {
                    String uniqueFileName = generateUniqueFileName(filePart.getSubmittedFileName());
                    File uploadFile = new File(getServletContext().getRealPath("/") + uploadDir, uniqueFileName);
                    filePart.write(uploadFile.getAbsolutePath());
                    return uploadFile;
                } else {
                    return null;
                }
            }
            
            Field[] fields = clazz.getDeclaredFields();
            Object object = clazz.getDeclaredConstructor().newInstance();
            Object parametre ;
            String methodName = "";
            Method m ;
            for (Field field : fields) {
                parametre = null;
                m = null;
                methodName = "";
                try {
                    // creation et invoke de la methode 
                    methodName = "set"+Utilitaire.capitalizeFirst(field.getName());
                    System.out.println("\n\nmethode : " + methodName);
                    m = clazz.getDeclaredMethod(methodName , field.getType());
                    if (Utilitaire.isTypeGenerique(field.getType())) {
                        System.out.println("Type generique");
                        parametre = Utilitaire.cast(request.getParameter(prefixe+"."+field.getName()) , field.getType());
                    } else if (field.getType().isArray()){

                        if (Utilitaire.isTypeGeneriqueArray(field.getType())) {
                            System.out.println("Array de type generique");
                            String[] args = request.getParameterValues(prefixe+"."+field.getName());
                            Object[] paramArray = new Object[args.length];
                            for (int i = 0; i < args.length; i++) {
                                paramArray[i] = Utilitaire.cast(args[i], field.getType().getComponentType());
                            }

                            parametre = paramArray;
                        } else {
                            parametre = assignToArray(request, response, field.getType().getComponentType(), prefixe+"."+field.getName());
                        }
                    } else if (List.class.isAssignableFrom(field.getType())) {
                        if (Utilitaire.isListObjectGenerique(field.getGenericType())) {
                            System.out.println("List de type generique");
                            String[] args = request.getParameterValues(prefixe+"."+field.getName());
                            List<Object> paramArray = (List<Object>) field.getType().getDeclaredConstructor().newInstance();
                            for (int i = 0; i < args.length; i++) {
                                Object val = Utilitaire.cast(args[i], (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                                paramArray.add(val);
                            }

                            parametre = paramArray;
                        } else {
                            System.out.println("List d'objet ");
                            List<Object> list = (List<Object>) field.getType().getDeclaredConstructor().newInstance();
                            Object[] paramArray = assignToArray(request, response, (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], prefixe+"."+field.getName());
                            for (Object o : paramArray) {
                                list.add(o);
                            }
                            parametre = list;
                        }
                    } else if(File.class.isAssignableFrom(field.getType())){
                        Part filePart = request.getPart(prefixe+"."+field.getName());
                        if (filePart != null && filePart.getSize() > 0) {
                            String uniqueFileName = generateUniqueFileName(filePart.getSubmittedFileName());
                            File uploadFile = new File(getServletContext().getRealPath("/") + uploadDir, uniqueFileName);
                            filePart.write(uploadFile.getAbsolutePath());
                            parametre = uploadFile;
                        } else {
                            parametre = null;
                        }
                    }
                    else {
                        parametre = assignToObject(request, response, field.getType(), prefixe+"."+field.getName());
                    } 
                    m.invoke(object, parametre);

                } catch (NoSuchMethodException nsm){
                    System.err.println("Setteur Innexistant dans la class "+clazz.getName()+" pour le champ "+field.getName());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    throw ex;
                }
                
            }
            return object;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }
    private Object[] assignToArray(HttpServletRequest request, HttpServletResponse response , Class<?> clazz , String prefixe) throws Exception{
        try {
            Object[] array = null;
            // type generique
            if(Utilitaire.isTypeGenerique(clazz)){
                String[] paramValues = request.getParameterValues(prefixe);
                array = new Object[paramValues.length];
                for (int i = 0; i < paramValues.length; i++) {
                    array[i] = Utilitaire.cast(paramValues[i], clazz);
                }
                return array;
            // file
            } else if (clazz.equals(File.class)){
                Collection<Part> parts = request.getParts();
                List<Part> fileParts = parts.stream()
                    .filter(part -> part.getName().equals(prefixe) && part.getSize() > 0)
                    .toList();
                array = new File[fileParts.size()];
                for (int i = 0; i < fileParts.size(); i++) {
                    Part filePart = fileParts.get(i);
                    String uniqueFileName = generateUniqueFileName(filePart.getSubmittedFileName());
                    File uploadFile = new File(getServletContext().getRealPath("/") + uploadDir, uniqueFileName);
                    filePart.write(uploadFile.getAbsolutePath());
                    array[i] = uploadFile;
                }
                return array;
            // object 
            } else {
                array = new Object[getTailleArray(request, response, prefixe)];
                for (int i = 0; i < array.length; i++) {
                    System.out.println("\n\nAssignation de l'element d'index "+i+" du tableau de "+clazz.getName()+"\n\n");
                    array[i] = assignToObject(request, response, clazz, prefixe+"["+i+"]");

                }
                return array;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private int getTailleArray(HttpServletRequest request, HttpServletResponse response,String prefixe)throws Exception{
        Enumeration<String> parameterNames = request.getParameterNames();
        int taille = 0;
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (paramName.startsWith(prefixe)) {
                try {
                    String[] parts = paramName.split(prefixe + "\\[");
                    taille = Math.max(taille , parts.length > 1 ? Integer.parseInt(parts[1].split("\\]")[0]) : 0);
                } catch (Exception e) {
                }
            }
        } 
        return taille+1; 
    }

    private String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            return System.currentTimeMillis() + "_file";
        }
        
        // Securiser le nom en enlevant les caracteres dangereux et path traversal
        String safeName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        safeName = safeName.replaceAll("\\.\\.", "_");
        
        // Separer nom et extension
        int lastDot = safeName.lastIndexOf('.');
        String name = lastDot > 0 ? safeName.substring(0, lastDot) : safeName;
        String extension = lastDot > 0 ? safeName.substring(lastDot) : "";
        
        // GGenerer un nom unique : timestamp_nomOriginal.ext
        return System.currentTimeMillis() + "_" + name + extension;
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

            e.printStackTrace();
            // PrintWriter out = response.getWriter();
            // out.print(e);
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

    
    
}