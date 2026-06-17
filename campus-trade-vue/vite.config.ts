import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

function resolveProxyTarget(env: Record<string, string>) {
  const raw = (env.VITE_DEV_PROXY_TARGET || env.VITE_API_BASE_URL || "").trim();
  if (!raw || !/^https?:\/\//i.test(raw)) {
    return "http://localhost:8082";
  }
  return raw.replace(/\/api\/?$/, "");
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, "");
  const proxyTarget = resolveProxyTarget(env);

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src"),
      },
    },
    server: {
      proxy: {
        "/api": {
          target: proxyTarget,
          changeOrigin: true,
        },
        "/upload": {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
