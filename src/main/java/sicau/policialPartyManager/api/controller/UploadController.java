package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.UploadResult;
import sicau.policialPartyManager.service.FileStorageService;

/**
 * 通用文件上传（一期本地磁盘，#33 附件场景）。登录即可。
 */
@Tag(name = "文件上传", description = "本地存储的通用附件上传（思想汇报等）")
@RestController
@RequestMapping("/api/v4/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传附件",
            description = "multipart 字段 file；category 目前仅 thought；扩展名限 pdf/doc/docx/jpg/png；"
                    + "返回 file: 前缀相对路径，提交表单时原样带回")
    @PostMapping
    public Result<UploadResult> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "thought") String category) {
        return Result.ok(fileStorageService.save(file, category));
    }
}
