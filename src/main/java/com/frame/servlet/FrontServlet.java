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
import java.util.Properties;

import com.frame.annotation.AnnotationGetteur;
import com.frame.annotation.Mapping;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class FrontServlet extends HttpServlet {

    private List<Mapping> mappings;
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
            if (packageName==null) {
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
            if ( file.isDirectory()){
                if((new File(appPath,resourcePath+"index.jsp")).exists()){
                    response.setContentType("text/html;charset=UTF-8");

                    try (InputStream in = context.getResourceAsStream(resourcePath+"index.jsp");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        PrintWriter out = response.getWriter()) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            out.println(line);
                        }
                        return;
                    }
                } else if((new File(appPath,resourcePath+"index.html")).exists()){
                    response.setContentType("text/html;charset=UTF-8");

                    try (InputStream in = context.getResourceAsStream(resourcePath+"index.html");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        PrintWriter out = response.getWriter()) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            out.println(line);
                        }
                        return;
                    }
                }



            } else if( file.isFile()){
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
            
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.println("Méthode : " + method);
            out.println("URL : " + url);
        }
        
    }

    private void print(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            
            
        PrintWriter out = response.getWriter();
        try {
            out.println("package name : "+packageName);
            for (Mapping mapping : mappings) {
                out.println("class : "+mapping.getClazz().getName() + " ; method : " + mapping.getMethod().getName() + " ; Path : " + mapping.getPath() + " ; Type annotation : "+mapping.getAnnotation());
            }
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
            
            print(request, response);
        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            out.println(e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        

        try {
            
            work(request, response);
        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            out.println(e.getMessage());
        }
    }
}