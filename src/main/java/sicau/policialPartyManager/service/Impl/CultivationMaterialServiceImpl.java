package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.model.entity.CultivationMaterial;
import sicau.policialPartyManager.repository.CultivationMaterialMapper;
import sicau.policialPartyManager.service.BranchScopeService;

import java.util.List;
import java.util.Set;

/**
 * 培养材料（#73）。
 * <p>文件本身复用既有上传端点产出的 {@code file:} 相对路径，不新建存储通道。
 * <p>删除是软删除（status=0），底层文件与记录都不物理清除。
 */
@Service
@RequiredArgsConstructor
public class CultivationMaterialServiceImpl {

    private static final Set<String> TYPES = Set.of(
            "入党申请书", "思想汇报", "政审材料", "培训证书", "其他");

    private final CultivationMaterialMapper materialMapper;
    private final BranchScopeService branchScopeService;

    @Transactional
    public Long upload(sicau.policialPartyManager.model.records.User operator, Long userId,
                       String materialType, String file, String fileName, String remark) {
        branchScopeService.requireManageableUser(operator, userId);
        if (!StringUtils.hasText(materialType) || !TYPES.contains(materialType.trim())) {
            throw new IllegalArgumentException("材料类型不合法，可选：" + String.join("/", TYPES));
        }
        if (!StringUtils.hasText(file) || !file.trim().startsWith("file:")) {
            throw new IllegalArgumentException("材料地址必须为 file: 前缀的相对路径");
        }
        CultivationMaterial m = new CultivationMaterial();
        m.setUserId(userId);
        m.setMaterialType(materialType.trim());
        m.setFile(file.trim());
        m.setFileName(fileName);
        m.setUploaderId(operator.userId());
        m.setRemark(remark);
        m.setStatus(1);
        materialMapper.insert(m);
        return m.getId();
    }

    public List<CultivationMaterial> listOf(sicau.policialPartyManager.model.records.User operator, Long userId) {
        branchScopeService.requireManageableUser(operator, userId);
        return materialMapper.selectList(new LambdaQueryWrapper<CultivationMaterial>()
                .eq(CultivationMaterial::getUserId, userId)
                .eq(CultivationMaterial::getStatus, 1)
                .orderByDesc(CultivationMaterial::getId));
    }

    /** 软删除；记录仍在，只是不再出现在列表里。 */
    @Transactional
    public void remove(sicau.policialPartyManager.model.records.User operator, Long materialId) {
        CultivationMaterial m = materialMapper.selectById(materialId);
        if (m == null || m.getStatus() == null || m.getStatus() != 1) {
            throw new IllegalArgumentException("材料不存在");
        }
        branchScopeService.requireManageableUser(operator, m.getUserId());
        m.setStatus(0);
        materialMapper.updateById(m);
    }

    /** 供培养档案聚合使用：不做范围校验（调用方已校验）。 */
    public List<CultivationMaterial> listByUserRaw(Long userId) {
        return materialMapper.selectList(new LambdaQueryWrapper<CultivationMaterial>()
                .eq(CultivationMaterial::getUserId, userId)
                .eq(CultivationMaterial::getStatus, 1)
                .orderByDesc(CultivationMaterial::getId));
    }
}
