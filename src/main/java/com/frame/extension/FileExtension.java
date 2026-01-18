package com.frame.extension;

public class FileExtension {
    
    String fileName;
    String fileExtension;

    
    public FileExtension(String fileName, String fileExtension) {
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }
    
    public FileExtension(String fullFileName, byte[] content) {
        this.fileName = fullFileName;
        
        int lastDot = fullFileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fullFileName.length() - 1) {
            this.fileExtension = fullFileName.substring(lastDot + 1);
        } else {
            this.fileExtension = "";
        }
    }
    
    public FileExtension(){}
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFileExtension() {
        return fileExtension;
    }
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
}
