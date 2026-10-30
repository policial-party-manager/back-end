package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件存储（一期，#33 附件；后续可平替对象存储）。
 * 存 {@code app.upload.dir} 下按 {@code thought/yyyy/MM/uuid.ext} 组织，
 * 返回 {@code file:} 前缀相对路径（与活动封面/公告内容的前缀约定一致）。
 */
public interface FileStorageService {

    /** 保存并返回引用；扩展名白名单之外抛 IllegalArgumentException（400） */
    UploadResult save(MultipartFile file, String category);
}
