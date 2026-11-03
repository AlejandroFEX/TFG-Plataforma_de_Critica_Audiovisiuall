package es.udc.fic.fictionary.user;

import es.udc.fic.fictionary.common.BadRequestException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private final Path uploadDir;

    public AvatarStorageService(
            @Value("${fictionary.uploads.dir:uploads/avatars}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("El fichero está vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imágenes");
        }
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(filename));
            return "/avatars/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el fichero", e);
        }
    }

    public void delete(String avatarUrl) {
        if (avatarUrl == null) return;
        String filename = avatarUrl.replace("/avatars/", "");
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            // ignorar si no existe
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}