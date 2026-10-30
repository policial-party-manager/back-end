package sicau.policialPartyManager.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.UploadResult;
import sicau.policialPartyManager.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘实现。安全约束：
 * <ul>
 *   <li>扩展名白名单（pdf/doc/docx/jpg/png），文件名完全由服务端生成（UUID），
 *       原始名仅用于回显，绝不参与路径拼接；</li>
 *   <li>category 必须是白名单标识，杜绝 {@code ../} 注入；</li>
 *   <li>落盘后校验最终路径仍在存储根内（双保险）。</li>
 * </ul>
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "jpg", "png");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("thought");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    public LocalFileStorageServiceImpl(@Value("${app.upload.dir:./data/uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public UploadResult save(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("不支持的上传分类");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(original);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("不支持的文件类型，仅允许 pdf/doc/docx/jpg/png");
        }

        String relativeDir = category + "/" + LocalDate.now().format(MONTH);
        String filename = UUID.randomUUID().toString().replace("-", "")
                + "." + extension.toLowerCase(Locale.ROOT);
        Path target = root.resolve(relativeDir).resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("存储路径解析异常");
        }
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", target, e);
            throw new IllegalStateException("附件保存失败，请重试");
        }
        return new UploadResult("file:" + relativeDir + "/" + filename, original, file.getSize());
    }
}
