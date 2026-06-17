package com.campus.twohand.system.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.system.entity.SystemConfig;
import com.campus.twohand.system.repo.SystemConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private static final String DEFAULT_SITE_NAME = "校园二手";
    private static final String DEFAULT_SITE_SUBTITLE = "校园二手交易平台";
    private static final String DEFAULT_NOTICE_TITLE = "平台公告";
    private static final String DEFAULT_NOTICE_CONTENT = "欢迎使用校园二手交易平台，请文明交易，注意账号与资金安全。";
    private static final boolean DEFAULT_PRODUCT_AUDIT_ENABLED = true;
    private static final int DEFAULT_MAX_UPLOAD_COUNT = 6;
    private static final String DEFAULT_CONTACT_INFO = "请联系平台管理员";
    private static final String DEFAULT_SITE_DESC = "轻量发布、同校沟通、线下见面更安心，适合校园二手交易场景。";

    private final SystemConfigRepository systemConfigRepository;
    private final SessionAuthSupport sessionAuthSupport;

    public ApiResp<?> get(HttpServletRequest request) {
        sessionAuthSupport.requireSuperAdmin(request);
        return ApiResp.ok(toResp(ensureConfig()));
    }

    @Transactional
    public ApiResp<?> save(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireSuperAdmin(request);

        String siteName = trim(body.get("siteName"));
        String siteSubtitle = trim(body.get("siteSubtitle"));
        String noticeTitle = trim(body.get("noticeTitle"));
        String noticeContent = trim(body.get("noticeContent"));
        Boolean productAuditEnabled = parseBoolean(body.get("productAuditEnabled"));
        Integer maxUploadCount = parseInteger(body.get("maxUploadCount"));
        String contactInfo = trim(body.get("contactInfo"));
        String siteDesc = trim(body.get("siteDesc"));

        if (siteName.isEmpty()) {
            return ApiResp.fail("平台名称不能为空");
        }
        if (siteName.length() > 100) {
            return ApiResp.fail("平台名称长度不能超过100个字符");
        }
        if (siteSubtitle.length() > 255) {
            return ApiResp.fail("平台副标题长度不能超过255个字符");
        }
        if (noticeTitle.length() > 100) {
            return ApiResp.fail("首页公告标题长度不能超过100个字符");
        }
        if (noticeContent.length() > 2000) {
            return ApiResp.fail("首页公告内容长度不能超过2000个字符");
        }
        if (productAuditEnabled == null) {
            return ApiResp.fail("商品审核开关不能为空");
        }
        if (maxUploadCount == null) {
            return ApiResp.fail("单次最多上传图片数不能为空");
        }
        if (maxUploadCount < 1 || maxUploadCount > 20) {
            return ApiResp.fail("单次最多上传图片数需在1到20之间");
        }
        if (contactInfo.length() > 255) {
            return ApiResp.fail("平台联系方式长度不能超过255个字符");
        }
        if (siteDesc.length() > 4000) {
            return ApiResp.fail("平台说明长度不能超过4000个字符");
        }

        SystemConfig config = ensureConfig();
        config.setSiteName(siteName);
        config.setSiteSubtitle(siteSubtitle);
        config.setNoticeTitle(noticeTitle);
        config.setNoticeContent(noticeContent);
        config.setProductAuditEnabled(productAuditEnabled);
        config.setMaxUploadCount(maxUploadCount);
        config.setContactInfo(contactInfo);
        config.setSiteDesc(siteDesc);
        config.setUpdatedAt(LocalDateTime.now());
        systemConfigRepository.save(config);
        return ApiResp.ok(toResp(config));
    }

    @Transactional
    protected SystemConfig ensureConfig() {
        return systemConfigRepository.findTopByOrderByIdAsc().orElseGet(this::createDefaultConfig);
    }

    private SystemConfig createDefaultConfig() {
        LocalDateTime now = LocalDateTime.now();
        SystemConfig config = new SystemConfig();
        config.setSiteName(DEFAULT_SITE_NAME);
        config.setSiteSubtitle(DEFAULT_SITE_SUBTITLE);
        config.setNoticeTitle(DEFAULT_NOTICE_TITLE);
        config.setNoticeContent(DEFAULT_NOTICE_CONTENT);
        config.setProductAuditEnabled(DEFAULT_PRODUCT_AUDIT_ENABLED);
        config.setMaxUploadCount(DEFAULT_MAX_UPLOAD_COUNT);
        config.setContactInfo(DEFAULT_CONTACT_INFO);
        config.setSiteDesc(DEFAULT_SITE_DESC);
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        return systemConfigRepository.save(config);
    }

    private Map<String, Object> toResp(SystemConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", config.getId());
        data.put("siteName", valueOrDefault(config.getSiteName(), DEFAULT_SITE_NAME));
        data.put("siteSubtitle", valueOrDefault(config.getSiteSubtitle(), DEFAULT_SITE_SUBTITLE));
        data.put("noticeTitle", valueOrDefault(config.getNoticeTitle(), DEFAULT_NOTICE_TITLE));
        data.put("noticeContent", valueOrDefault(config.getNoticeContent(), DEFAULT_NOTICE_CONTENT));
        data.put("productAuditEnabled", config.getProductAuditEnabled() != null ? config.getProductAuditEnabled() : DEFAULT_PRODUCT_AUDIT_ENABLED);
        data.put("maxUploadCount", config.getMaxUploadCount() != null ? config.getMaxUploadCount() : DEFAULT_MAX_UPLOAD_COUNT);
        data.put("contactInfo", valueOrDefault(config.getContactInfo(), DEFAULT_CONTACT_INFO));
        data.put("siteDesc", valueOrDefault(config.getSiteDesc(), DEFAULT_SITE_DESC));
        data.put("createdAt", config.getCreatedAt());
        data.put("updatedAt", config.getUpdatedAt());
        return data;
    }

    private String valueOrDefault(String value, String defaultValue) {
        String text = trim(value);
        return text.isEmpty() ? defaultValue : text;
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        if ("1".equals(text) || "true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }
}
