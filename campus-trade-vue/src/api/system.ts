import { http } from "@/api/http";

export interface SystemConfigResp {
  id: number;
  siteName: string;
  siteSubtitle?: string;
  noticeTitle?: string;
  noticeContent?: string;
  productAuditEnabled: boolean;
  maxUploadCount: number;
  contactInfo?: string;
  siteDesc?: string;
  createdAt?: string;
  updatedAt?: string;
}

export const apiAdminSystemConfigGet = () =>
  http.get<SystemConfigResp>("/admin/system/config");

export const apiAdminSystemConfigSave = (data: {
  siteName: string;
  siteSubtitle?: string;
  noticeTitle?: string;
  noticeContent?: string;
  productAuditEnabled: boolean;
  maxUploadCount: number;
  contactInfo?: string;
  siteDesc?: string;
}) => http.put<SystemConfigResp>("/admin/system/config", data);

