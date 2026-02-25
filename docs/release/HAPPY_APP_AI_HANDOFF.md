# Happy App 发布中心接入执行单（给 AI 助手）

本执行单用于让 `happy` 项目的 AI 助手快速完成接入，不需要再自行设计规则。  
统一规则来源：`docs/release/APK_RELEASE_CENTER_SPEC.md`。

## 目标

把 `happy` 的 APK 更新分发统一到发布中心 `:18080`，并产出标准 `latest.json`。

## 必做项（按顺序）

1. 将 `happy` APK 放入 canonical 路径：

```text
/opt/apk-downloads/releases/happy-app/android/stable/<version_name>/happy-app-<version_name>.apk
```

2. 生成并写入：
   - `sha256`
   - `file_size_bytes`
   - `updated_at`（ISO8601，UTC）

3. 更新：

```text
/opt/apk-downloads/releases/happy-app/android/stable/latest.json
```

4. 如果 `happy` 有业务 API（例如 `GET /api/v1/app/android/latest`），将其 `download_url` 切到发布中心 URL（`18080`），不要再指向业务服务本机下载路径。

## `latest.json` 模板

```json
{
  "app_id": "happy-app",
  "platform": "android",
  "channel": "stable",
  "version_code": 1,
  "version_name": "0.1.0",
  "file_name": "happy-app-0.1.0.apk",
  "file_size_bytes": 0,
  "sha256": "REPLACE_WITH_REAL_SHA256",
  "updated_at": "2026-02-25T03:30:00.000Z",
  "download_url": "http://118.196.100.121:18080/releases/happy-app/android/stable/0.1.0/happy-app-0.1.0.apk",
  "release_notes": "填写本次更新说明"
}
```

## 服务器命令参考（可直接执行）

> 注意：将 `<version_name>` 与 `<version_code>` 替换为真实版本。

```bash
APP_ID=happy-app
CHANNEL=stable
VER_NAME=<version_name>
VER_CODE=<version_code>
SRC_APK=/opt/apk-downloads/happy-app/happy-app-debug.apk
DEST_DIR=/opt/apk-downloads/releases/$APP_ID/android/$CHANNEL/$VER_NAME
DEST_APK=$DEST_DIR/$APP_ID-$VER_NAME.apk
LATEST_JSON=/opt/apk-downloads/releases/$APP_ID/android/$CHANNEL/latest.json

mkdir -p "$DEST_DIR" "$(dirname "$LATEST_JSON")"
cp "$SRC_APK" "$DEST_APK"

SIZE=$(stat -c%s "$DEST_APK")
SHA=$(sha256sum "$DEST_APK" | awk '{print $1}')
UPDATED=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")

cat > "$LATEST_JSON" <<JSON
{
  "app_id": "$APP_ID",
  "platform": "android",
  "channel": "$CHANNEL",
  "version_code": $VER_CODE,
  "version_name": "$VER_NAME",
  "file_name": "$APP_ID-$VER_NAME.apk",
  "file_size_bytes": $SIZE,
  "sha256": "$SHA",
  "updated_at": "$UPDATED",
  "download_url": "http://118.196.100.121:18080/releases/$APP_ID/android/$CHANNEL/$VER_NAME/$APP_ID-$VER_NAME.apk",
  "release_notes": "happy app release $VER_NAME"
}
JSON
```

## 验收标准（必须同时满足）

1. `latest.json` 可访问：

```bash
curl -sS http://118.196.100.121:18080/releases/happy-app/android/stable/latest.json
```

2. APK 可下载且 `HTTP 200`：

```bash
curl -I -sS http://118.196.100.121:18080/releases/happy-app/android/stable/<version_name>/happy-app-<version_name>.apk
```

3. 若 `happy` 有更新 API，则其返回的 `download_url` 必须是 `18080` 的发布中心 URL。
