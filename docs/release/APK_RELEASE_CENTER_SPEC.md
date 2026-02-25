# APK 发布中心规范（0326）

本规范用于统一管理多个 Android 应用的安装包发布与更新元数据，适用于：

- `chef-ascend`
- `happy-app`
- `local-relay-android`

目标是将“业务服务 API”与“APK 分发”解耦，后续所有应用都遵循同一套发布策略。

## 1. 发布中心基地址

- 基地址：`http://118.196.100.121:18080`
- 发布中心只负责静态文件分发（APK + 元数据）

## 2. 目录与命名规范（必须遵循）

### 2.1 Canonical 路径（新规范）

```text
/releases/<app_id>/android/<channel>/<version_name>/<apk_file>
/releases/<app_id>/android/<channel>/latest.json
```

说明：

- `app_id`：`chef-ascend` / `happy-app` / `local-relay-android`
- `channel`：`stable` 或 `beta`
- `version_name`：例如 `0.5.0`
- `apk_file`：建议 `<app_id>-<version_name>.apk`

### 2.2 兼容路径（历史保留）

当前线上可继续保留：

- `http://118.196.100.121:18080/happy-app/happy-app-debug.apk`
- `http://118.196.100.121:18080/local-relay-android/local-relay-android-debug.apk`

但新版本发布建议同步写入 canonical 路径，逐步迁移。

## 3. `latest.json` 统一格式

`latest.json` 存放在：

```text
/releases/<app_id>/android/<channel>/latest.json
```

示例：

```json
{
  "app_id": "chef-ascend",
  "platform": "android",
  "channel": "stable",
  "version_code": 5,
  "version_name": "0.5.0",
  "file_name": "chef-ascend-0.5.0.apk",
  "file_size_bytes": 21034071,
  "sha256": "REPLACE_WITH_REAL_SHA256",
  "updated_at": "2026-02-25T03:10:00.000Z",
  "download_url": "http://118.196.100.121:18080/releases/chef-ascend/android/stable/0.5.0/chef-ascend-0.5.0.apk",
  "release_notes": "新增实时下载速度与下载百分比展示"
}
```

## 4. 发布流程（标准）

1. 上传 APK 到版本目录（不可覆盖旧版本）。
2. 计算并记录 `sha256`、`file_size_bytes`。
3. 更新 `latest.json` 指向新版本。
4. 校验下载链接可达、`Content-Length` 正确。
5. 再触发客户端“检查更新”验证。

## 5. 回滚策略

- 不删除历史 APK。
- 回滚时仅修改 `latest.json` 到旧版本即可。

## 6. AI 助手协作约束（必须）

- 不直接复用旧版本 `version_code`。
- 不覆盖历史版本 APK 文件。
- 任何发布必须同时更新 `latest.json`。
- `download_url` 必须指向发布中心（`18080`），不得指向业务 API 服务地址。
- 发布后必须做两类校验：
  - `latest.json` 返回版本正确；
  - APK URL 返回 `200` 且可下载。

## 7. Chef Ascend 服务接入约定

Chef Ascend 的更新接口 `GET /api/v1/app/android/latest` 仅做“元数据聚合”，下载链接应由发布中心提供。

推荐配置：

```env
ANDROID_APK_DOWNLOAD_URL=http://118.196.100.121:18080/releases/chef-ascend/android/stable/0.5.0/chef-ascend-0.5.0.apk
ANDROID_APK_FILE_SIZE_BYTES=21034071
ANDROID_APK_UPDATED_AT=2026-02-25T03:10:00.000Z
```

这样即使业务服务本地无 APK 文件，也能返回可用更新信息。
