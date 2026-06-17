import { ElMessage, ElMessageBox } from "element-plus";
import { getApiErrorMessage } from "@/utils/apiError";

type ConfirmType = "warning" | "info" | "success" | "error";

export type ConfirmActionOptions = {
  message: string;
  title?: string;
  type?: ConfirmType;
  confirmButtonText?: string;
  cancelButtonText?: string;
  successMessage?: string;
  errorMessage?: string;
  showError?: boolean;
  action: () => Promise<void> | void;
  onSuccess?: () => Promise<void> | void;
  onError?: (error: unknown) => Promise<void> | void;
};

export function useConfirmAction() {
  async function runConfirmAction(options: ConfirmActionOptions): Promise<boolean> {
    const {
      message,
      title = "提示",
      type = "warning",
      confirmButtonText = "确定",
      cancelButtonText = "取消",
      successMessage,
      errorMessage = "操作失败",
      showError = true,
      action,
      onSuccess,
      onError,
    } = options;

    try {
      await ElMessageBox.confirm(message, title, {
        type,
        confirmButtonText,
        cancelButtonText,
      });
    } catch {
      return false;
    }

    try {
      await action();
      if (successMessage) {
        ElMessage.success(successMessage);
      }
      if (onSuccess) {
        await onSuccess();
      }
      return true;
    } catch (error) {
      if (onError) {
        await onError(error);
      } else if (showError) {
        ElMessage.error(getApiErrorMessage(error, errorMessage));
      }
      return false;
    }
  }

  return {
    runConfirmAction,
  };
}
