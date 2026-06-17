-- =====================================================================
-- 该文件仅为局部兼容脚本，当前不会自动执行，不作为完整数据库初始化脚本。
-- 完整初始化请使用：sql/项目数据库初始化.sql
-- =====================================================================

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  site_name VARCHAR(100) NOT NULL COMMENT '平台名称',
  site_subtitle VARCHAR(255) DEFAULT NULL COMMENT '平台副标题',
  notice_title VARCHAR(100) DEFAULT NULL COMMENT '首页公告标题',
  notice_content TEXT COMMENT '首页公告内容',
  product_audit_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开启商品发布审核',
  max_upload_count INT NOT NULL DEFAULT 6 COMMENT '单次最多上传图片数',
  contact_info VARCHAR(255) DEFAULT NULL COMMENT '平台联系方式',
  site_desc TEXT COMMENT '平台说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO system_config (
  id,
  site_name,
  site_subtitle,
  notice_title,
  notice_content,
  product_audit_enabled,
  max_upload_count,
  contact_info,
  site_desc
)
SELECT
  1,
  '校园二手',
  '校园二手交易平台',
  '平台公告',
  '欢迎使用校园二手交易平台',
  1,
  6,
  '',
  '请文明交易，注意账号与财产安全。'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM system_config WHERE id = 1
);

CREATE TABLE IF NOT EXISTS report (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  reporter_id BIGINT NOT NULL COMMENT '举报人ID',
  product_id BIGINT NOT NULL COMMENT '被举报商品ID',
  reason VARCHAR(100) NOT NULL COMMENT '举报原因',
  detail TEXT DEFAULT NULL COMMENT '补充说明',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING / VALID / INVALID / HANDLED',
  handle_remark TEXT DEFAULT NULL COMMENT '管理员处理备注',
  handled_by BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  PRIMARY KEY (id),
  KEY idx_report_product_created (product_id, created_at),
  KEY idx_report_reporter_status (reporter_id, status, created_at),
  KEY idx_report_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';
