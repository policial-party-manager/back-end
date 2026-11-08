"""把新增端点按既有风格追加进 openapi.yaml（#68 各票的契约同步）。

只追加缺失的 path，不重排、不改动既有条目。
"""
import io
import re

PATH = "openapi.yaml"

# (path, method, summary, description, tag)
NEW = [
    ("/api/v4/members/page", "get", "成员分页（按操作者范围）",
     "超管全院；支委限本支部。支持 keyword 与年级/专业/学院/政治身份筛选", "成员管理（支委）"),
    ("/api/v4/members/{id}", "get", "成员详情（按操作者范围）", "越权与真不存在同文案", "成员管理（支委）"),
    ("/api/v4/members/{id}/contact", "put", "修改成员联系方式",
     "仅手机号/邮箱/备注；部分更新，未传字段不动，空串清空", "成员管理（支委）"),
    ("/api/v4/members/export", "get", "导出成员名单（xlsx）", "受与列表相同的筛选条件约束", "成员管理（支委）"),
    ("/api/v4/members/{userId}/mentors", "get", "查看成员当前培养联系人", "", "培养域"),
    ("/api/v4/members/{userId}/mentors", "post", "指派培养联系人",
     "每人最多 2 名在职联系人；已存在则幂等", "培养域"),
    ("/api/v4/members/{userId}/mentors/{mentorId}", "delete", "移除培养联系人", "", "培养域"),
    ("/api/v4/members/{userId}/mentors/batch", "post", "批量指派培养联系人",
     "逐条成功；单次最多 200 条", "培养域"),
    ("/api/v4/members/{userId}/mentees", "get", "按培养联系人反查带教成员",
     "支委只返回本支部成员", "培养域"),
    ("/api/v4/members/{userId}/materials", "get", "成员培养材料列表", "", "培养域"),
    ("/api/v4/members/{userId}/materials", "post", "登记培养材料",
     "先调 /api/v4/uploads 拿到 file: 路径，再用本接口登记归属与类型", "培养域"),
    ("/api/v4/members/{userId}/materials/{materialId}", "delete", "删除培养材料（软删除）", "", "培养域"),
    ("/api/v4/admin/users/batch/status", "post", "批量停用/恢复",
     "status：1 启用 / 2 停用；单次最多 200 条；逐条返回成功与失败明细", "成员批量操作"),
    ("/api/v4/admin/users/batch/branch", "post", "批量调整所属支部",
     "仅超级管理员；单次最多 200 条", "成员批量操作"),
    ("/api/v4/admin/activities/{activityId}/leaves", "post", "录入请假",
     "同一成员同一活动只保留一条生效记录；重复录入即更新理由", "活动考勤"),
    ("/api/v4/admin/activities/{activityId}/leaves/{userId}", "delete", "撤销请假", "", "活动考勤"),
    ("/api/v4/admin/activities/{activityId}/attendance", "get", "考勤统计",
     "签到/请假/缺席三张名单 + 参与率；考勤四态为正常签到/迟到/请假/缺席；"
     "应到不可界定（活动无参与范围）时缺席名单为空、参与率为 null", "活动考勤"),
    ("/api/v4/activities/{activityId}/sign-qr", "get", "生成/获取当前签到二维码",
     "返回 token 与过期秒数；同一窗口内复用同一 token，过期后自动轮换", "二维码签到"),
    ("/api/v4/activities/{activityId}/sign-by-qr", "post", "扫码签到",
     "携带当前有效 token；过期或伪造被拒。重复签到返回「您已完成签到」", "二维码签到"),
    ("/api/v4/activities/{activityId}/signups/page", "get", "活动报名记录分页（按操作者范围）", "",
     "活动域（支委）"),
    ("/api/v4/activities/{activityId}/signs/page", "get", "活动签到记录分页（按操作者范围）", "",
     "活动域（支委）"),
    ("/api/v4/cultivation-profiles/{userId}", "get", "查询成员完整培养档案",
     "基本信息 + 培养联系人 + 身份变更历史 + 思想汇报 + 活动签到/请假 + 学习记录 + 培养材料 + 统计摘要；"
     "各区块无数据时返回空集合", "电子培养档案"),
    ("/api/v4/cultivation-profiles/mine", "get", "查询自己的完整培养档案", "", "电子培养档案"),
    ("/api/v4/statistics/dashboard", "get", "首页待办与阶段分布",
     "待审核思想汇报数、待审批身份变更数（支委限本支部），以及各政治身份人数分布", "首页统计"),
    ("/api/v4/statistics/distribution", "get", "阶段分布（按维度切分）",
     "dimension 可选 college / major / className / grade，缺省 college", "首页统计"),
]


def block(path, method, summary, description, tag):
    e = io.StringIO()
    e.write(f"  {path}:\n")
    e.write(f"    {method}:\n")
    e.write(f"      tags:\n")
    e.write(f'        - "{tag}"\n')
    e.write(f'      summary: "{summary}"\n')
    if description:
        e.write(f'      description: "{description}"\n')
    e.write("      responses:\n")
    e.write('        "200":\n')
    e.write('          description: ""\n')
    e.write("          content:\n")
    e.write("            application/json:\n")
    e.write("              schema:\n")
    e.write('                type: "object"\n')
    e.write("                properties:\n")
    e.write("                  code:\n")
    e.write('                    type: "integer"\n')
    e.write("                  message:\n")
    e.write('                    type: "string"\n')
    e.write("                  data:\n")
    e.write('                    type: "object"\n')
    return e.getvalue()


def main():
    with io.open(PATH, encoding="utf-8") as f:
        text = f.read()

    existing = set(re.findall(r"(?m)^  (/[^:]+):", text))

    # 按 path 聚合（同一 path 可挂多个 method）
    from collections import OrderedDict
    grouped = OrderedDict()
    for p, m, s, d, t in NEW:
        grouped.setdefault(p, []).append((m, s, d, t))

    appended = []
    for p, ops in grouped.items():
        if p in existing:
            continue
        buf = io.StringIO()
        buf.write(f"  {p}:\n")
        for m, s, d, t in ops:
            sub = block(p, m, s, d, t)
            # 去掉 block() 写出的 path 行，只保留 method 及其内容
            lines = sub.split("\n")
            buf.write("\n".join(lines[1:]))
        appended.append(buf.getvalue())

    if not appended:
        print("no new paths")
        return

    # 必须插进 paths 块内部：定位顶层 "components:"，在它之前插入。
    # 直接追加到文件末尾会落进 components 块，破坏 YAML 结构。
    marker = "\ncomponents:"
    idx = text.find(marker)
    if idx < 0:
        raise SystemExit("找不到顶层 components:，拒绝写入")

    insertion = ""
    for a in appended:
        insertion += a

    text = text[:idx + 1] + insertion + text[idx + 1:]
    with io.open(PATH, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print(f"appended {len(appended)} paths before components:")


if __name__ == "__main__":
    main()
