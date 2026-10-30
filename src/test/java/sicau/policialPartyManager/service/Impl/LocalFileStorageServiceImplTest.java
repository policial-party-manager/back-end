package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import sicau.policialPartyManager.api.dto.UploadResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地附件存储的安全与行为测试（#33）。
 */
class LocalFileStorageServiceImplTest {

    @TempDir
    Path dir;

    LocalFileStorageServiceImpl storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorageServiceImpl(dir.toString());
    }

    @Test
    void savesPdfUnderDatePathWithFilePrefix() throws Exception {
        var file = new MockMultipartFile("file", "汇报 件.PDF", "application/pdf", "%PDF-1.4".getBytes());

        UploadResult result = storage.save(file, "thought");

        assertTrue(result.getUrl().startsWith("file:thought/"), result.getUrl());
        assertTrue(result.getUrl().matches("file:thought/\\d{4}/\\d{2}/[0-9a-f]{32}\\.pdf"), result.getUrl());
        assertEquals("汇报 件.PDF", result.getOriginalName());
        Path stored = dir.resolve(result.getUrl().substring("file:".length()));
        assertTrue(Files.exists(stored));
        assertEquals("%PDF-1.4", Files.readString(stored));
    }

    @Test
    void rejectsDangerousExtension() {
        var file = new MockMultipartFile("file", "evil.jsp", "application/octet-stream", new byte[]{1});
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> storage.save(file, "thought"));
        assertTrue(e.getMessage().contains("不支持的文件类型"), e.getMessage());
    }

    @Test
    void rejectsExtensionlessName() {
        var file = new MockMultipartFile("file", "README", "text/plain", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> storage.save(file, "thought"));
    }

    @Test
    void rejectsPathTraversalInNameButKeepsStorageInsideRoot() throws Exception {
        // 原始名里的路径成分不参与落盘（服务端文件名=UUID），净化后仅回显
        var file = new MockMultipartFile("file", "../../evil.pdf", "application/pdf", "x".getBytes());
        UploadResult result = storage.save(file, "thought");
        Path stored = dir.resolve(result.getUrl().substring("file:".length())).normalize();
        assertTrue(stored.startsWith(dir.toAbsolutePath().normalize()));
    }

    @Test
    void rejectsUnknownCategory() {
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> storage.save(file, "../etc"));
    }

    @Test
    void rejectsEmptyFile() {
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{});
        assertThrows(IllegalArgumentException.class, () -> storage.save(file, "thought"));
    }
}
