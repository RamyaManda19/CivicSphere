package com.civicsphere.controller;

import com.civicsphere.config.AppConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller serving uploaded complaint evidence photos securely.
 */
@WebServlet(name = "ImageServlet", urlPatterns = {"/uploads/*"})
public class ImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prevent path traversal
        String filename = Paths.get(pathInfo).getFileName().toString();
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String uploadDirConfig = AppConfig.getUploadDir();
        Path uploadPath;
        if (Paths.get(uploadDirConfig).isAbsolute()) {
            uploadPath = Paths.get(uploadDirConfig);
        } else {
            String appRealPath = req.getServletContext().getRealPath("");
            uploadPath = Paths.get(appRealPath != null ? appRealPath : ".", uploadDirConfig);
        }

        File file = uploadPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = req.getServletContext().getMimeType(file.getName());
        if (contentType == null) {
            if (filename.endsWith(".png")) contentType = "image/png";
            else if (filename.endsWith(".webp")) contentType = "image/webp";
            else contentType = "image/jpeg";
        }

        resp.setContentType(contentType);
        resp.setContentLengthLong(file.length());
        resp.setHeader("Cache-Control", "public, max-age=86400");

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

