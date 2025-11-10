package com.frame.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.rowset.serial.SerialException;

import com.frame.annotation.AnnotationGetteur;
import com.frame.model.Mapping;
import com.frame.model.ModelView;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class FrontServlet extends HttpServlet {

    private Map<String , Mapping > mappings;
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

            mappings=AnnotationGetteur.getAllMapping(packageName);
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
            
            
            Mapping mapping = mappings.get("/"+resourcePath);
            if (mapping==null) {
                PrintWriter out = response.getWriter();
                out.print("erreur 404 Not found");
            }else {
                if(mapping.getMethod().getReturnType().equals(String.class)){
                    try {
                        response.setContentType("text/plain");
                        response.setCharacterEncoding("UTF-8");
                        
                        PrintWriter out = response.getWriter();
                        Object instance = mapping.getClazz().getDeclaredConstructor().newInstance();
                        
                        String retour = (String)mapping.getMethod().invoke(instance);
                        out.print(retour);
                        
                        return;
                    } catch (Exception e) {
                        throw new ServletException(e);
                    }
                } else if (mapping.getMethod().getReturnType().equals(ModelView.class)) {
                    try {
                        
                        Object instance = mapping.getClazz().getDeclaredConstructor().newInstance();
                        
                        ModelView retour = (ModelView)mapping.getMethod().invoke(instance);
                        
                        RequestDispatcher dispatcher = request.getRequestDispatcher(retour.getView());
                        dispatcher.forward(request, response);
                    } catch (Exception e) {
                        throw new ServletException(e);
                    }

                    
                }else {
                    PrintWriter out = response.getWriter();
                    out.print("erreur 500");
                }
            }
            
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







    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        

        try {
            work(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.print(e);
            // throw new ServletException(e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        

        try {
            
            work(request, response);
        } catch (Exception e) {

            e.printStackTrace();
            PrintWriter out = response.getWriter();
            out.print(e);
            // throw new ServletException(e);
        }
    }
}