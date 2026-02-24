export const renderAdminDishPage = (): string => {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Chef Ascend 菜品管理台</title>
  <style>
    :root {
      --bg: #f7f7f3;
      --surface: #ffffff;
      --border: #dfdfd7;
      --primary: #da5f27;
      --primary-dark: #af4818;
      --text: #22221d;
      --subtle: #646454;
      --ok: #2e8f4e;
      --danger: #c43a2f;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
      color: var(--text);
      background: linear-gradient(165deg, #fff8f1 0%, var(--bg) 36%);
    }
    .page { max-width: 1380px; margin: 0 auto; padding: 20px 18px 28px; }
    .topbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 14px; }
    .title { margin: 0; font-size: 30px; font-weight: 800; letter-spacing: 0.4px; }
    .subtitle { margin: 4px 0 0; color: var(--subtle); font-size: 14px; }
    .grid { display: grid; grid-template-columns: 420px minmax(680px, 1fr); gap: 14px; align-items: start; }
    .card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 14px;
      box-shadow: 0 6px 24px rgba(34, 34, 29, 0.06);
      overflow: hidden;
    }
    .card h2 { margin: 0; font-size: 18px; }
    .card-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid var(--border); background: #fffdf8; }
    .card-body { padding: 12px 14px 16px; }
    .search-row { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; margin-bottom: 10px; }
    .muted { color: var(--subtle); font-size: 13px; }
    .stats { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
    .stat-pill {
      border: 1px solid var(--border);
      border-radius: 999px;
      padding: 5px 10px;
      font-size: 12px;
      color: var(--subtle);
      background: #fff;
    }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 8px; border-bottom: 1px solid #f0efe8; text-align: left; vertical-align: top; font-size: 13px; }
    th { background: #fcfbf6; color: #4e4e42; font-weight: 600; }
    tr.selectable { cursor: pointer; transition: background 0.14s ease; }
    tr.selectable:hover { background: #fff5ea; }
    tr.selected { background: #ffe9d5; }
    .badge {
      border-radius: 999px;
      padding: 2px 8px;
      font-size: 11px;
      font-weight: 700;
      display: inline-block;
    }
    .badge.active { background: #e6f6eb; color: var(--ok); }
    .badge.inactive { background: #fdeceb; color: var(--danger); }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 180px 180px;
      gap: 10px;
      margin-bottom: 10px;
    }
    .field { display: flex; flex-direction: column; gap: 5px; }
    .field label { font-size: 12px; color: var(--subtle); }
    input[type="text"], input[type="number"], textarea, select {
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 8px 9px;
      font-size: 14px;
      width: 100%;
      background: #fff;
      color: var(--text);
    }
    textarea { min-height: 82px; resize: vertical; }
    .form-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
    button {
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 8px 12px;
      cursor: pointer;
      background: #fff;
      color: var(--text);
      font-weight: 600;
      font-size: 13px;
    }
    button.primary {
      border-color: var(--primary);
      background: var(--primary);
      color: #fff;
    }
    button.primary:hover { background: var(--primary-dark); border-color: var(--primary-dark); }
    button.danger { border-color: #efc1bd; color: var(--danger); }
    button.small { padding: 5px 7px; font-size: 12px; }
    .section-title { margin: 12px 0 8px; font-size: 14px; font-weight: 700; color: #404033; }
    .checkbox-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 6px 8px; margin-bottom: 10px; }
    .checkbox-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #35352e; }
    .table-wrap { border: 1px solid var(--border); border-radius: 10px; overflow: hidden; background: #fff; margin-bottom: 8px; }
    .status {
      min-height: 32px;
      border-radius: 8px;
      border: 1px dashed var(--border);
      background: #fffdf8;
      padding: 8px 10px;
      color: #4a4a3e;
      font-size: 13px;
      margin-bottom: 10px;
      white-space: pre-wrap;
    }
    .json-box {
      border: 1px solid var(--border);
      border-radius: 10px;
      background: #fbfbf9;
      max-height: 240px;
      overflow: auto;
      font-size: 12px;
      padding: 10px;
      color: #40403a;
    }
    @media (max-width: 1100px) {
      .grid { grid-template-columns: 1fr; }
      .form-grid { grid-template-columns: 1fr 1fr; }
    }
    @media (max-width: 700px) {
      .form-grid { grid-template-columns: 1fr; }
      .checkbox-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .topbar { flex-direction: column; align-items: flex-start; }
    }
  </style>
</head>
<body>
  <div class="page">
    <div class="topbar">
      <div>
        <h1 class="title">菜品数据库可视化管理</h1>
        <p class="subtitle">可直接管理 dishes / dish_steps / dish_category_links，适合日常运营维护。</p>
      </div>
      <div class="muted">API: <code>/api/v1/admin/*</code></div>
    </div>

    <div class="grid">
      <section class="card">
        <div class="card-header">
          <h2>菜品列表</h2>
          <button id="new-dish-btn">+ 新建菜品</button>
        </div>
        <div class="card-body">
          <div class="search-row">
            <input id="search-input" type="text" placeholder="搜索菜名或 slug" />
            <button id="search-btn">搜索</button>
            <button id="refresh-btn">刷新</button>
          </div>
          <label class="checkbox-item" style="margin-bottom: 10px;">
            <input type="checkbox" id="include-inactive" checked />
            显示下架菜品
          </label>

          <div class="stats">
            <span class="stat-pill" id="stat-total">总数: 0</span>
            <span class="stat-pill" id="stat-active">上架: 0</span>
            <span class="stat-pill" id="stat-inactive">下架: 0</span>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width:58px;">ID</th>
                  <th>菜名</th>
                  <th style="width:72px;">难度</th>
                  <th style="width:90px;">步骤数</th>
                  <th style="width:90px;">今日人数</th>
                  <th style="width:82px;">状态</th>
                </tr>
              </thead>
              <tbody id="dish-table-body"></tbody>
            </table>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-header">
          <h2>菜品编辑</h2>
          <div class="muted" id="editing-hint">新建模式</div>
        </div>
        <div class="card-body">
          <div id="status-box" class="status">就绪：请选择一个菜品，或点击“新建菜品”。</div>

          <div class="form-grid">
            <div class="field">
              <label for="dish-name">菜品名称</label>
              <input id="dish-name" type="text" placeholder="例如：番茄炒蛋" />
            </div>
            <div class="field">
              <label for="dish-slug">slug（英文标识）</label>
              <input id="dish-slug" type="text" placeholder="例如：tomato-scrambled-eggs" />
            </div>
            <div class="field">
              <label for="dish-difficulty">难度</label>
              <select id="dish-difficulty">
                <option value="1">1</option>
                <option value="2">2</option>
                <option value="3">3</option>
                <option value="4">4</option>
                <option value="5">5</option>
              </select>
            </div>
          </div>

          <div class="form-grid">
            <div class="field">
              <label for="dish-cover-url">封面图 URL（可选）</label>
              <input id="dish-cover-url" type="text" placeholder="https://..." />
            </div>
            <div class="field">
              <label for="dish-seconds">预计总时长（秒）</label>
              <input id="dish-seconds" type="number" min="1" value="600" />
            </div>
            <div class="field">
              <label for="dish-active">是否上架</label>
              <select id="dish-active">
                <option value="1">上架</option>
                <option value="0">下架</option>
              </select>
            </div>
          </div>

          <div class="field" style="margin-bottom: 10px;">
            <label for="dish-description">菜品描述</label>
            <textarea id="dish-description" placeholder="可选：写做菜风味、口感、适合人群等"></textarea>
          </div>

          <div class="section-title">分类</div>
          <div id="category-list" class="checkbox-list"></div>

          <div class="section-title">食材清单</div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width: 42%;">食材名</th>
                  <th style="width: 42%;">用量</th>
                  <th style="width: 16%;">操作</th>
                </tr>
              </thead>
              <tbody id="ingredients-body"></tbody>
            </table>
          </div>
          <button id="add-ingredient-btn" class="small">+ 添加食材</button>

          <div class="section-title">步骤（已有菜品暂不支持减少步骤数量）</div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width: 7%;">序号</th>
                  <th style="width: 18%;">标题</th>
                  <th style="width: 39%;">说明</th>
                  <th style="width: 13%;">计时(秒)</th>
                  <th style="width: 13%;">提醒</th>
                  <th style="width: 10%;">操作</th>
                </tr>
              </thead>
              <tbody id="steps-body"></tbody>
            </table>
          </div>
          <button id="add-step-btn" class="small">+ 添加步骤</button>

          <div class="form-actions">
            <button id="save-btn" class="primary">保存菜品</button>
            <button id="toggle-active-btn" class="danger">上架 / 下架切换</button>
            <button id="reload-detail-btn">重新加载详情</button>
          </div>

          <div class="section-title">数据库预览（当前编辑对象）</div>
          <pre id="json-preview" class="json-box">{}</pre>
        </div>
      </section>
    </div>
  </div>

  <script>
    (function () {
      var state = {
        categories: [],
        dishes: [],
        selectedDishId: null
      };

      var remindModeOptions = ["NONE", "SOUND", "VIBRATION", "BOTH"];
      var remindModeLabel = {
        NONE: "无",
        SOUND: "声音",
        VIBRATION: "震动",
        BOTH: "声音+震动"
      };

      var statusBox = document.getElementById("status-box");
      var tableBody = document.getElementById("dish-table-body");
      var categoryList = document.getElementById("category-list");
      var ingredientsBody = document.getElementById("ingredients-body");
      var stepsBody = document.getElementById("steps-body");
      var jsonPreview = document.getElementById("json-preview");
      var editingHint = document.getElementById("editing-hint");
      var statTotal = document.getElementById("stat-total");
      var statActive = document.getElementById("stat-active");
      var statInactive = document.getElementById("stat-inactive");

      function setStatus(message, isError) {
        statusBox.textContent = message;
        statusBox.style.borderColor = isError ? "#efc1bd" : "#dfdfd7";
        statusBox.style.background = isError ? "#fff5f4" : "#fffdf8";
      }

      async function request(path, options) {
        var response = await fetch(path, options || {});
        if (!response.ok) {
          var message = "请求失败: HTTP " + response.status;
          try {
            var body = await response.json();
            if (body && body.message) {
              message = body.message;
            }
            if (body && Array.isArray(body.issues) && body.issues.length > 0) {
              message = message + "\\n" + body.issues.map(function (issue) {
                return "- " + issue.path + ": " + issue.message;
              }).join("\\n");
            }
          } catch (_) {}
          throw new Error(message);
        }
        if (response.status === 204) {
          return null;
        }
        return response.json();
      }

      function getValue(id) {
        return document.getElementById(id).value;
      }

      function setValue(id, value) {
        document.getElementById(id).value = value;
      }

      function setStats() {
        var total = state.dishes.length;
        var active = state.dishes.filter(function (item) { return item.is_active; }).length;
        var inactive = total - active;
        statTotal.textContent = "总数: " + total;
        statActive.textContent = "上架: " + active;
        statInactive.textContent = "下架: " + inactive;
      }

      function createCategoryCheckbox(category, checked) {
        var label = document.createElement("label");
        label.className = "checkbox-item";
        var checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.dataset.categoryId = String(category.id);
        checkbox.checked = checked;
        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(category.name + " (#" + category.id + ")"));
        return label;
      }

      function renderCategories(selectedIds) {
        categoryList.innerHTML = "";
        var selectedSet = new Set(selectedIds || []);
        state.categories.forEach(function (category) {
          categoryList.appendChild(createCategoryCheckbox(category, selectedSet.has(category.id)));
        });
      }

      function renderDishTable() {
        tableBody.innerHTML = "";
        state.dishes.forEach(function (dish) {
          var tr = document.createElement("tr");
          tr.className = "selectable" + (state.selectedDishId === dish.id ? " selected" : "");
          tr.dataset.dishId = String(dish.id);

          var statusClass = dish.is_active ? "badge active" : "badge inactive";
          var statusText = dish.is_active ? "上架" : "下架";
          var categoryText = (dish.categories || []).join(" / ");

          tr.innerHTML =
            "<td>" + dish.id + "</td>" +
            "<td><div><strong>" + escapeHtml(dish.name) + "</strong></div><div class=\\"muted\\">" +
            escapeHtml(categoryText || "-") + "</div></td>" +
            "<td>" + dish.difficulty + "</td>" +
            "<td>" + dish.step_count + "</td>" +
            "<td>" + dish.today_cook_count + "</td>" +
            "<td><span class=\\"" + statusClass + "\\">" + statusText + "</span></td>";

          tr.addEventListener("click", function () {
            loadDishDetail(dish.id).catch(function (error) {
              setStatus(error.message, true);
            });
          });

          tableBody.appendChild(tr);
        });
        setStats();
      }

      function addIngredientRow(data) {
        var item = data || { name: "", amount: "" };
        var tr = document.createElement("tr");
        tr.innerHTML =
          "<td><input type=\\"text\\" class=\\"ingredient-name\\" value=\\"" + escapeAttr(item.name || "") + "\\" /></td>" +
          "<td><input type=\\"text\\" class=\\"ingredient-amount\\" value=\\"" + escapeAttr(item.amount || "") + "\\" /></td>" +
          "<td><button type=\\"button\\" class=\\"small\\">删除</button></td>";
        tr.querySelector("button").addEventListener("click", function () {
          tr.remove();
        });
        ingredientsBody.appendChild(tr);
      }

      function addStepRow(data) {
        var item = data || {
          step_no: stepsBody.children.length + 1,
          title: "",
          instruction: "",
          timer_seconds: 120,
          remind_mode: "BOTH"
        };

        var tr = document.createElement("tr");
        tr.innerHTML =
          "<td class=\\"step-no\\"></td>" +
          "<td><input type=\\"text\\" class=\\"step-title\\" value=\\"" + escapeAttr(item.title || "") + "\\" /></td>" +
          "<td><textarea class=\\"step-instruction\\" style=\\"min-height: 62px;\\">" + escapeHtml(item.instruction || "") + "</textarea></td>" +
          "<td><input type=\\"number\\" class=\\"step-timer\\" min=\\"0\\" value=\\"" + Number(item.timer_seconds || 0) + "\\" /></td>" +
          "<td><select class=\\"step-remind\\"></select></td>" +
          "<td><button type=\\"button\\" class=\\"small\\">删除</button></td>";

        var select = tr.querySelector(".step-remind");
        remindModeOptions.forEach(function (mode) {
          var option = document.createElement("option");
          option.value = mode;
          option.textContent = remindModeLabel[mode] || mode;
          option.selected = mode === item.remind_mode;
          select.appendChild(option);
        });

        tr.querySelector("button").addEventListener("click", function () {
          tr.remove();
          syncStepNo();
        });

        stepsBody.appendChild(tr);
        syncStepNo();
      }

      function syncStepNo() {
        Array.prototype.forEach.call(stepsBody.children, function (row, index) {
          var stepCell = row.querySelector(".step-no");
          if (stepCell) {
            stepCell.textContent = String(index + 1);
          }
        });
      }

      function collectCategoryIds() {
        return Array.prototype.map.call(
          categoryList.querySelectorAll("input[type='checkbox']:checked"),
          function (element) { return Number(element.dataset.categoryId); }
        );
      }

      function collectIngredients() {
        return Array.prototype.flatMap.call(ingredientsBody.children, function (row) {
          var name = (row.querySelector(".ingredient-name").value || "").trim();
          var amount = (row.querySelector(".ingredient-amount").value || "").trim();
          if (!name || !amount) {
            return [];
          }
          return [{ name: name, amount: amount }];
        });
      }

      function collectSteps() {
        return Array.prototype.flatMap.call(stepsBody.children, function (row) {
          var title = (row.querySelector(".step-title").value || "").trim();
          var instruction = (row.querySelector(".step-instruction").value || "").trim();
          var timerSeconds = Number(row.querySelector(".step-timer").value || "0");
          var remindMode = row.querySelector(".step-remind").value;
          if (!title || !instruction) {
            return [];
          }
          return [{
            title: title,
            instruction: instruction,
            timer_seconds: timerSeconds,
            remind_mode: remindMode
          }];
        });
      }

      function collectPayload() {
        var steps = collectSteps();
        if (steps.length === 0) {
          throw new Error("至少需要一个完整步骤（标题+说明）");
        }

        var name = getValue("dish-name").trim();
        if (!name) {
          throw new Error("菜品名称不能为空");
        }

        var payload = {
          name: name,
          slug: getValue("dish-slug").trim() || null,
          description: getValue("dish-description").trim() || null,
          difficulty: Number(getValue("dish-difficulty")),
          estimated_total_seconds: Number(getValue("dish-seconds")),
          cover_image_url: getValue("dish-cover-url").trim() || null,
          category_ids: collectCategoryIds(),
          ingredients: collectIngredients(),
          steps: steps,
          is_active: getValue("dish-active") === "1"
        };

        if (!Number.isInteger(payload.estimated_total_seconds) || payload.estimated_total_seconds <= 0) {
          throw new Error("预计总时长必须是正整数（秒）");
        }

        jsonPreview.textContent = JSON.stringify(payload, null, 2);
        return payload;
      }

      function resetForm() {
        state.selectedDishId = null;
        setValue("dish-name", "");
        setValue("dish-slug", "");
        setValue("dish-description", "");
        setValue("dish-cover-url", "");
        setValue("dish-difficulty", "1");
        setValue("dish-seconds", "600");
        setValue("dish-active", "1");
        ingredientsBody.innerHTML = "";
        stepsBody.innerHTML = "";
        addIngredientRow();
        addStepRow();
        renderCategories([]);
        editingHint.textContent = "新建模式";
        jsonPreview.textContent = "{}";
        renderDishTable();
      }

      function fillForm(detail) {
        state.selectedDishId = detail.id;
        setValue("dish-name", detail.name || "");
        setValue("dish-slug", detail.slug || "");
        setValue("dish-description", detail.description || "");
        setValue("dish-cover-url", detail.cover_image_url || "");
        setValue("dish-difficulty", String(detail.difficulty));
        setValue("dish-seconds", String(detail.estimated_total_seconds));
        setValue("dish-active", detail.is_active ? "1" : "0");

        ingredientsBody.innerHTML = "";
        stepsBody.innerHTML = "";
        (detail.ingredients || []).forEach(addIngredientRow);
        (detail.steps || []).forEach(addStepRow);
        if ((detail.ingredients || []).length === 0) {
          addIngredientRow();
        }
        if ((detail.steps || []).length === 0) {
          addStepRow();
        }
        renderCategories(detail.category_ids || []);

        editingHint.textContent = "编辑中：#" + detail.id + " " + detail.name;
        jsonPreview.textContent = JSON.stringify(detail, null, 2);
        renderDishTable();
      }

      async function loadCategories() {
        state.categories = await request("/api/v1/admin/categories");
        renderCategories([]);
      }

      async function loadDishList() {
        var keyword = getValue("search-input").trim();
        var includeInactive = document.getElementById("include-inactive").checked ? "1" : "0";
        var query = new URLSearchParams();
        if (keyword) {
          query.set("keyword", keyword);
        }
        query.set("include_inactive", includeInactive);

        var response = await request("/api/v1/admin/dishes?" + query.toString());
        state.dishes = response.items || [];
        renderDishTable();
      }

      async function loadDishDetail(dishId) {
        var detail = await request("/api/v1/admin/dishes/" + dishId);
        fillForm(detail);
        setStatus("已加载菜品 #" + dishId + "。", false);
      }

      async function onSave() {
        try {
          var payload = collectPayload();
          setStatus("正在保存...", false);
          if (state.selectedDishId) {
            var updated = await request("/api/v1/admin/dishes/" + state.selectedDishId, {
              method: "PUT",
              headers: { "content-type": "application/json" },
              body: JSON.stringify(payload)
            });
            await loadDishList();
            fillForm(updated);
            setStatus("保存成功：已更新菜品 #" + updated.id, false);
            return;
          }

          var created = await request("/api/v1/admin/dishes", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(payload)
          });
          await loadDishList();
          fillForm(created);
          setStatus("创建成功：新菜品 ID = " + created.id, false);
        } catch (error) {
          setStatus(error.message || "保存失败", true);
        }
      }

      async function onToggleActive() {
        if (!state.selectedDishId) {
          setStatus("请先选择一个已存在的菜品。", true);
          return;
        }

        var nextValue = getValue("dish-active") === "1" ? false : true;
        try {
          await request("/api/v1/admin/dishes/" + state.selectedDishId + "/active", {
            method: "PATCH",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ is_active: nextValue })
          });
          await loadDishDetail(state.selectedDishId);
          await loadDishList();
          setStatus("状态已切换为：" + (nextValue ? "上架" : "下架"), false);
        } catch (error) {
          setStatus(error.message || "状态切换失败", true);
        }
      }

      function escapeHtml(value) {
        return String(value)
          .replaceAll("&", "&amp;")
          .replaceAll("<", "&lt;")
          .replaceAll(">", "&gt;");
      }

      function escapeAttr(value) {
        return escapeHtml(value).replaceAll("\\\"", "&quot;");
      }

      document.getElementById("search-btn").addEventListener("click", function () {
        loadDishList().catch(function (error) {
          setStatus(error.message, true);
        });
      });

      document.getElementById("refresh-btn").addEventListener("click", function () {
        loadDishList().catch(function (error) {
          setStatus(error.message, true);
        });
      });

      document.getElementById("include-inactive").addEventListener("change", function () {
        loadDishList().catch(function (error) {
          setStatus(error.message, true);
        });
      });

      document.getElementById("new-dish-btn").addEventListener("click", function () {
        resetForm();
        setStatus("已切换到新建模式。", false);
      });

      document.getElementById("add-ingredient-btn").addEventListener("click", function () {
        addIngredientRow();
      });

      document.getElementById("add-step-btn").addEventListener("click", function () {
        addStepRow();
      });

      document.getElementById("save-btn").addEventListener("click", function () {
        onSave();
      });

      document.getElementById("toggle-active-btn").addEventListener("click", function () {
        onToggleActive();
      });

      document.getElementById("reload-detail-btn").addEventListener("click", function () {
        if (!state.selectedDishId) {
          setStatus("当前是新建模式，没有可刷新的详情。", true);
          return;
        }
        loadDishDetail(state.selectedDishId).catch(function (error) {
          setStatus(error.message, true);
        });
      });

      (async function init() {
        try {
          await loadCategories();
          resetForm();
          await loadDishList();
          setStatus("加载完成。", false);
        } catch (error) {
          setStatus(error.message || "初始化失败", true);
        }
      })();
    })();
  </script>
</body>
</html>`;
};
