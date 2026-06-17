package com.campus.twohand.verify.service;

import com.campus.twohand.common.ApiResp;
import com.campus.twohand.common.SessionAuthSupport;
import com.campus.twohand.credit.service.CreditService;
import com.campus.twohand.product.repo.ProductRepository;
import com.campus.twohand.upload.service.UploadService;
import com.campus.twohand.user.entity.SysUser;
import com.campus.twohand.user.repo.SysUserRepository;
import com.campus.twohand.verify.entity.StudentVerify;
import com.campus.twohand.verify.repo.StudentVerifyRepository;
import com.campus.twohand.wanted.repo.WantedRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentVerifyService {

    private final StudentVerifyRepository studentVerifyRepository;
    private final SysUserRepository sysUserRepository;
    private final CreditService creditService;
    private final SessionAuthSupport sessionAuthSupport;
    private final ProductRepository productRepository;
    private final WantedRepository wantedRepository;
    private final UploadService uploadService;

    @Transactional
    public ApiResp<?> apply(HttpServletRequest request, Map<String, Object> req) {
        Long uid = sessionAuthSupport.requireUserId(request);
        doApply(uid, req);
        return ApiResp.ok("ok");
    }

    public ApiResp<?> my(HttpServletRequest request) {
        Long uid = sessionAuthSupport.requireUserId(request);
        return ApiResp.ok(doMy(uid));
    }

    public ApiResp<?> adminPage(HttpServletRequest request, int page, int size, String status, String keyword) {
        sessionAuthSupport.requireAdminId(request);
        return ApiResp.ok(doAdminPage(page, size, status, keyword));
    }

    public ApiResp<?> uploadMaterial(HttpServletRequest request, MultipartFile file) throws IOException {
        sessionAuthSupport.requireUserId(request);
        return uploadService.uploadVerifyMaterial(file);
    }

    @Transactional
    public ApiResp<?> adminApprove(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);
        Long id = parseLong(body.get("id"));
        if (id == null) {
            return ApiResp.fail("id不能为空");
        }
        doAdminApprove(id);
        return ApiResp.ok("ok");
    }

    @Transactional
    public ApiResp<?> adminReject(HttpServletRequest request, Map<String, Object> body) {
        sessionAuthSupport.requireAdminId(request);
        Long id = parseLong(body.get("id"));
        String reason = trim(body.get("reason"));
        if (id == null) {
            return ApiResp.fail("id不能为空");
        }
        if (reason.isEmpty()) {
            return ApiResp.fail("驳回原因不能为空");
        }
        doAdminReject(id, reason);
        return ApiResp.ok("ok");
    }

    private void doApply(Long uid, Map<String, Object> req) {
        SysUser user = sysUserRepository.findById(uid).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String school = trim(req.get("school"));
        String studentNo = trim(req.get("studentNo"));
        String realName = nullableTrim(req.get("realName"));
        String proofUrl = resolveProofUrl(req, uid);

        if (school.isEmpty()) {
            throw new RuntimeException("school不能为空");
        }
        if (studentNo.isEmpty()) {
            throw new RuntimeException("studentNo不能为空");
        }

        if ("PENDING".equalsIgnoreCase(user.getVerifyStatus())
                || studentVerifyRepository.existsByUserIdAndStatus(uid, "PENDING")) {
            throw new RuntimeException("认证申请审核中，请勿重复提交");
        }

        if (studentVerifyRepository.existsBySchoolAndStudentNoAndUserIdNotAndStatusIn(school, studentNo, uid, List.of("PENDING", "APPROVED"))
                || sysUserRepository.existsBySchoolAndStudentNoAndIdNot(school, studentNo, uid)) {
            throw new RuntimeException("该学号已被占用");
        }

        LocalDateTime now = LocalDateTime.now();

        StudentVerify verify = new StudentVerify();
        verify.setUserId(uid);
        verify.setSchool(school);
        verify.setStudentNo(studentNo);
        verify.setRealName(realName);
        verify.setProofUrl(proofUrl);
        verify.setStatus("PENDING");
        verify.setRejectReason(null);
        verify.setCreatedAt(now);
        verify.setUpdatedAt(now);
        studentVerifyRepository.save(verify);

        user.setVerifyStatus("PENDING");
        user.setSchool(school);
        user.setStudentNo(studentNo);
        user.setRealName(realName);
        sysUserRepository.save(user);
    }

    private Map<String, Object> doMy(Long uid) {
        SysUser user = sysUserRepository.findById(uid).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        StudentVerify latest = studentVerifyRepository.findTopByUserIdOrderByCreatedAtDesc(uid);
        Map<String, Object> data = new HashMap<>();
        data.put("verifyStatus", user.getVerifyStatus());
        data.put("latestApply", toVerifyApplyView(latest));
        return data;
    }

    private Map<String, Object> doAdminPage(int page, int size, String status, String keyword) {
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        Pageable pageable = PageRequest.of(p - 1, s);
        Page<Map<String, Object>> paged = studentVerifyRepository.adminPage(status, keyword, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("total", paged.getTotalElements());
        data.put("records", paged.getContent());
        return data;
    }

    private void doAdminApprove(Long id) {
        StudentVerify verify = studentVerifyRepository.findById(id).orElse(null);
        if (verify == null) {
            throw new RuntimeException("认证申请不存在");
        }
        if (!"PENDING".equalsIgnoreCase(verify.getStatus())) {
            throw new RuntimeException("仅待审核申请可通过");
        }

        SysUser user = sysUserRepository.findById(verify.getUserId()).orElse(null);
        if (user == null) {
            throw new RuntimeException("申请用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        verify.setStatus("APPROVED");
        verify.setRejectReason(null);
        verify.setUpdatedAt(now);
        studentVerifyRepository.save(verify);

        user.setVerifyStatus("VERIFIED");
        user.setVerifyTime(now);
        user.setSchool(verify.getSchool());
        user.setStudentNo(verify.getStudentNo());
        user.setRealName(verify.getRealName());
        sysUserRepository.save(user);

        productRepository.updateUnsoldSchoolNameBySellerId(user.getId(), verify.getSchool());
        wantedRepository.updateSchoolNameByUserId(user.getId(), verify.getSchool());

        creditService.rewardVerifyApprovedOnce(user.getId());
    }

    private void doAdminReject(Long id, String reason) {
        StudentVerify verify = studentVerifyRepository.findById(id).orElse(null);
        if (verify == null) {
            throw new RuntimeException("认证申请不存在");
        }
        if (!"PENDING".equalsIgnoreCase(verify.getStatus())) {
            throw new RuntimeException("仅待审核申请可驳回");
        }

        SysUser user = sysUserRepository.findById(verify.getUserId()).orElse(null);
        if (user == null) {
            throw new RuntimeException("申请用户不存在");
        }

        verify.setStatus("REJECTED");
        verify.setRejectReason(reason);
        verify.setUpdatedAt(LocalDateTime.now());
        studentVerifyRepository.save(verify);

        user.setVerifyStatus("REJECTED");
        sysUserRepository.save(user);
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String nullableTrim(Object value) {
        String text = trim(value);
        return text.isEmpty() ? null : text;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> toVerifyApplyView(StudentVerify verify) {
        if (verify == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", verify.getId());
        data.put("userId", verify.getUserId());
        data.put("school", verify.getSchool());
        data.put("studentNo", verify.getStudentNo());
        data.put("realName", verify.getRealName());
        data.put("proofUrl", verify.getProofUrl());
        data.put("status", verify.getStatus());
        data.put("rejectReason", verify.getRejectReason());
        data.put("createdAt", verify.getCreatedAt());
        return data;
    }

    private String resolveProofUrl(Map<String, Object> req, Long uid) {
        if (hasAnyKey(req, "proofUrl", "proof_url", "proof", "imageUrl", "materialUrl")) {
            return firstNullableTrim(req, "proofUrl", "proof_url", "proof", "imageUrl", "materialUrl");
        }
        StudentVerify latest = studentVerifyRepository.findTopByUserIdOrderByCreatedAtDesc(uid);
        return latest == null ? null : nullableTrim(latest.getProofUrl());
    }

    private boolean hasAnyKey(Map<String, Object> req, String... keys) {
        if (req == null) {
            return false;
        }
        for (String key : keys) {
            if (req.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private String firstNullableTrim(Map<String, Object> req, String... keys) {
        if (req == null) {
            return null;
        }
        for (String key : keys) {
            if (!req.containsKey(key)) {
                continue;
            }
            String text = nullableTrim(req.get(key));
            if (text != null) {
                return text;
            }
        }
        return null;
    }
}
