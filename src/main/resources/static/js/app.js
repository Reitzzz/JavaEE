const state = {
    me: null,
    categories: [],
    books: [],
    aiModels: [],
    users: [],
    aiSettings: null,
    loadingButton: null,
    activeDialog: null,
    lastFocusedElement: null
};

const message = document.querySelector("#message");

document.addEventListener("DOMContentLoaded", async () => {
    bindNavigation();
    bindActions();
    await run(async () => {
        await loadMe();
        await refreshCategories();
        await refreshBooks();
        if (isCurrentAdmin()) {
            await refreshAiSettings();
            await refreshAiModels();
            await refreshUsers();
        }
        await refreshBorrows(false);
    });
});

function bindNavigation() {
    document.querySelectorAll(".nav-item[data-tab]").forEach((item) => {
        item.addEventListener("click", () => {
            document.querySelectorAll(".nav-item[data-tab], .view").forEach((node) => node.classList.remove("active"));
            item.classList.add("active");
            document.querySelector(`#${item.dataset.tab}`).classList.add("active");
        });
    });
}

function bindActions() {
    document.querySelector("#searchBookBtn").addEventListener("click", () => refreshBooks());
    document.querySelector("#bookKeyword").addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            refreshBooks();
        }
    });
    document.querySelector("#userKeyword").addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            refreshUsers();
        }
    });
    document.querySelector("#userStatusFilter").addEventListener("change", () => refreshUsers());
    document.querySelector("#userBorrowStatusFilter").addEventListener("change", () => refreshUsers());
    document.querySelector("#loadMineBtn").addEventListener("click", () => refreshBorrows(false));
    document.querySelector("#loadAllBorrowsBtn").addEventListener("click", () => refreshBorrows(true));
    document.querySelector("#askAiBtn").addEventListener("click", askAi);
    document.querySelector("#categoryForm").addEventListener("submit", saveCategory);
    document.querySelector("#bookForm").addEventListener("submit", saveBook);
    document.querySelector("#aiSettingsForm").addEventListener("submit", saveAiSettings);
    document.querySelector("#aiModelForm").addEventListener("submit", saveAiModel);
    document.querySelector("#settingProviderSelect").addEventListener("change", () => renderAiSettings());
    document.querySelector("#modelProviderSelect").addEventListener("change", (event) => {
        const input = document.querySelector("#modelNameInput");
        if (event.target.value === "DeepSeek") {
            input.value = "deepseek-chat";
            input.placeholder = "例如：deepseek-chat";
        } else {
            input.value = "mimo-v2.5-pro";
            input.placeholder = "例如：mimo-v2.5-pro";
        }
    });
    document.querySelector("#userRows").addEventListener("click", (event) => {
        const btn = event.target.closest("button[data-action='view-user']");
        if (btn) {
            const userId = parseInt(btn.dataset.userId, 10);
            const user = state.users.find(u => u.id === userId);
            if (user) {
                showUserBorrows(user.id, user.displayName);
            }
        }
    });
    document.querySelector("#formDialogClose").addEventListener("click", closeFormDialog);
    document.querySelector("#confirmDialogClose").addEventListener("click", closeConfirmDialog);
    document.querySelector("#confirmDialogCancel").addEventListener("click", closeConfirmDialog);
    document.querySelector("#categoryBooksDialog").addEventListener("click", (event) => {
        if (event.target.id === "categoryBooksDialog") {
            closeCategoryBooksDialog();
        }
    });
    document.querySelector("#userDetailDialog").addEventListener("click", (event) => {
        if (event.target.id === "userDetailDialog") {
            closeUserDetailDialog();
        }
    });
    document.querySelector("#formDialog").addEventListener("click", (event) => {
        if (event.target.id === "formDialog") {
            closeFormDialog();
        }
    });
    document.querySelector("#confirmDialog").addEventListener("click", (event) => {
        if (event.target.id === "confirmDialog") {
            closeConfirmDialog();
        }
    });
    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") {
            return;
        }
        if (!document.querySelector("#formDialog").hidden) {
            closeFormDialog();
        } else if (!document.querySelector("#confirmDialog").hidden) {
            closeConfirmDialog();
        } else if (!document.querySelector("#categoryBooksDialog").hidden) {
            closeCategoryBooksDialog();
        } else if (!document.querySelector("#userDetailDialog").hidden) {
            closeUserDetailDialog();
        }
    });
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    if (response.redirected) {
        window.location.href = response.url;
        return null;
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data?.message || "请求失败，请稍后重试");
    }
    return data;
}

async function loadMe() {
    state.me = await api("/api/me");
    const isAdmin = isCurrentAdmin();
    document.querySelector("#currentUser").textContent = `${state.me.username} · ${isAdmin ? "管理员" : "读者"}`;
    document.querySelector("#roleLabel").textContent = isAdmin ? "管理员" : "读者";
    applyRoleVisibility();
    applyRoleLabels();
}

function applyRoleVisibility() {
    const isAdmin = isCurrentAdmin();
    document.querySelectorAll(".admin-only").forEach((node) => {
        node.style.display = isAdmin ? "" : "none";
    });
}

function applyRoleLabels() {
    const isAdmin = isCurrentAdmin();
    const bookTitle = isAdmin ? "图书管理" : "图书借阅";
    const categoryTitle = isAdmin ? "分类管理" : "类别查看";
    document.querySelector('[data-tab="books"] .nav-label').textContent = bookTitle;
    document.querySelector('[data-tab="categories"] .nav-label').textContent = categoryTitle;
    document.querySelector("#booksTitle").textContent = bookTitle;
    document.querySelector("#categoriesTitle").textContent = categoryTitle;
}

async function refreshUsers() {
    const keyword = document.querySelector("#userKeyword").value.trim().toLowerCase();
    const statusFilter = document.querySelector("#userStatusFilter").value;
    const borrowStatusFilter = document.querySelector("#userBorrowStatusFilter").value;
    
    let allUsers = await api("/api/users");
    const allBorrows = await api("/api/borrows"); 
    
    if (keyword) {
        allUsers = allUsers.filter(u => {
            if ((u.username && u.username.toLowerCase().includes(keyword)) ||
                (u.displayName && u.displayName.toLowerCase().includes(keyword))) {
                return true;
            }
            const userBorrows = allBorrows.filter(b => b.userId === u.id);
            return userBorrows.some(b => b.bookTitle && b.bookTitle.toLowerCase().includes(keyword));
        });
    }
    if (statusFilter) {
        allUsers = allUsers.filter(u => u.status === statusFilter);
    }
    if (borrowStatusFilter) {
        allUsers = allUsers.filter(u => {
            const userBorrows = allBorrows.filter(b => b.userId === u.id);
            if (borrowStatusFilter === 'OVERDUE') {
                return userBorrows.some(b => b.status === 'BORROWED' && new Date(b.dueAt) < new Date());
            } else if (borrowStatusFilter === 'BORROWED') {
                return userBorrows.some(b => b.status === 'BORROWED' && new Date(b.dueAt) >= new Date());
            } else {
                return userBorrows.some(b => b.status === borrowStatusFilter);
            }
        });
    }
    
    state.users = allUsers;
    renderUsers();
}

function renderUsers() {
    const rows = document.querySelector("#userRows");
    if (state.users.length === 0) {
        rows.innerHTML = emptyRow(8, "暂无用户数据");
        return;
    }
    rows.innerHTML = state.users.map((user) => `
        <tr>
            <td data-label="ID">${user.id}</td>
            <td data-label="账号">${escapeHtml(user.username)}</td>
            <td data-label="姓名">${escapeHtml(user.displayName)}</td>
            <td data-label="状态"><span class="badge ${user.status === 'NORMAL' ? 'success' : 'danger'}">${user.status === 'NORMAL' ? '正常' : '已拉黑'}</span></td>
            <td data-label="总借阅">${user.totalBorrows}</td>
            <td data-label="未归还"><span class="badge ${user.unreturnedBorrows > 0 ? 'warning' : 'muted'}">${user.unreturnedBorrows}</span></td>
            <td data-label="最近借阅时间">${formatTime(user.lastBorrowTime) || '-'}</td>
            <td class="actions">
                <button class="btn secondary" data-action="view-user" data-user-id="${user.id}" type="button">详情</button>
                ${user.status === 'NORMAL' 
                    ? `<button class="btn danger" onclick="toggleUserBan(${user.id}, true)" type="button">拉黑</button>`
                    : `<button class="btn secondary" onclick="toggleUserBan(${user.id}, false)" type="button">解封</button>`
                }
            </td>
        </tr>
    `).join("");
}

function toggleUserBan(id, isBan) {
    const actionName = isBan ? "拉黑" : "解封";
    openConfirmDialog({
        title: `确认${actionName}用户`,
        message: `您确定要${actionName}该用户吗？${isBan ? '拉黑后用户将无法再借阅图书。' : '解封后用户将恢复借阅权限。'}`,
        confirmText: `确认${actionName}`,
        onConfirm: async () => {
            await api(`/api/users/${id}/${isBan ? 'ban' : 'unban'}`, { method: "POST" });
            showMessage(`用户已${actionName}`);
            await refreshUsers();
        }
    });
}

async function showUserBorrows(userId, displayName) {
    const rows = await api("/api/borrows");
    const userBorrows = rows.filter(r => r.userId === userId);
    
    document.querySelector("#userDetailTitle").textContent = `${displayName} 的借阅详情`;
    document.querySelector("#userDetailSummary").textContent = `共 ${userBorrows.length} 条记录`;
    
    const tbody = document.querySelector("#userDetailRows");
    if (userBorrows.length === 0) {
        tbody.innerHTML = emptyRow(6, "该用户暂无借阅记录");
    } else {
        tbody.innerHTML = userBorrows.map(record => {
            const isOverdue = record.status === 'BORROWED' && new Date(record.dueAt) < new Date();
            let statusBadge = "muted";
            let statusText = "已归还";
            if (record.status === 'BORROWED') {
                if (isOverdue) {
                    statusBadge = "danger";
                    statusText = "逾期";
                } else {
                    statusBadge = "success";
                    statusText = "借阅中";
                }
            }
            return `
            <tr>
                <td data-label="书名">${escapeHtml(record.bookTitle)}</td>
                <td data-label="作者">${escapeHtml(record.bookAuthor || '--')}</td>
                <td data-label="借出时间">${formatTime(record.borrowedAt)}</td>
                <td data-label="应还时间">${formatTime(record.dueAt)}</td>
                <td data-label="实际归还">${formatTime(record.returnedAt) || '-'}</td>
                <td data-label="状态"><span class="badge ${statusBadge}">${statusText}</span></td>
            </tr>
            `;
        }).join("");
    }
    
    const dialog = document.querySelector("#userDetailDialog");
    state.lastFocusedElement = document.activeElement;
    state.activeDialog = dialog;
    dialog.hidden = false;
}

function closeUserDetailDialog() {
    const dialog = document.querySelector("#userDetailDialog");
    dialog.hidden = true;
    restoreDialogFocus(dialog);
}

async function refreshCategories() {
    state.categories = await api("/api/categories");
    renderCategories();
    fillCategoryOptions();
    updateMetrics();
}

function renderCategories() {
    const rows = document.querySelector("#categoryRows");
    if (state.categories.length === 0) {
        rows.innerHTML = emptyRow(4, "暂无分类数据");
        return;
    }
    rows.innerHTML = state.categories.map((category) => `
        <tr>
            <td data-label="编号">${category.id}</td>
            <td data-label="名称">${escapeHtml(category.name)}</td>
            <td data-label="描述">${escapeHtml(category.description)}</td>
            <td class="actions">
                <button class="btn secondary" onclick="viewCategoryBooks(${category.id})" type="button">查看</button>
                <button class="btn warning admin-only" onclick="editCategory(${category.id})" type="button">编辑</button>
                <button class="btn danger admin-only" onclick="deleteCategory(${category.id})" type="button">删除</button>
            </td>
        </tr>
    `).join("");
    applyRoleVisibility();
}

function fillCategoryOptions() {
    const select = document.querySelector("#bookForm select[name='categoryId']");
    select.innerHTML = state.categories.map((category) => (
        `<option value="${category.id}">${escapeHtml(category.name)}</option>`
    )).join("");
}

async function refreshBooks() {
    const keyword = document.querySelector("#bookKeyword").value.trim();
    state.books = await api(`/api/books${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""}`);
    renderBooks();
    updateMetrics();
}

async function loadAllBooks() {
    return api("/api/books");
}

function renderBooks() {
    const rows = document.querySelector("#bookRows");
    const isAdmin = isCurrentAdmin();
    if (state.books.length === 0) {
        rows.innerHTML = emptyRow(6, "暂无图书数据");
        return;
    }
    rows.innerHTML = state.books.map((book) => `
        <tr>
            <td data-label="ISBN"><code>${escapeHtml(book.isbn)}</code></td>
            <td data-label="书名">${escapeHtml(book.title)}</td>
            <td data-label="作者">${escapeHtml(book.author)}</td>
            <td data-label="分类">${escapeHtml(book.categoryName)}</td>
            <td data-label="库存"><span class="badge ${book.availableCopies > 0 ? "success" : "muted"}">${book.availableCopies}/${book.totalCopies}</span></td>
            <td class="actions">
                <button class="btn secondary" onclick="borrowBook(${book.id})" type="button" ${book.availableCopies <= 0 ? "disabled" : ""}>借阅</button>
                ${isAdmin ? `<button class="btn warning" onclick="editBook(${book.id})" type="button">编辑</button><button class="btn danger" onclick="deleteBook(${book.id})" type="button">删除</button>` : ""}
            </td>
        </tr>
    `).join("");
}

async function refreshBorrows(all) {
    const rows = await api(all ? "/api/borrows" : "/api/borrows/mine");
    const target = document.querySelector("#borrowRows");
    if (rows.length === 0) {
        target.innerHTML = emptyRow(6, all ? "暂无借阅记录" : "你还没有借阅记录");
        return;
    }
    target.innerHTML = rows.map((record) => `
        <tr>
            <td data-label="读者">${escapeHtml(record.displayName)}</td>
            <td data-label="图书">${escapeHtml(record.bookTitle)}</td>
            <td data-label="借出时间">${formatTime(record.borrowedAt)}</td>
            <td data-label="应还时间">${formatTime(record.dueAt)}</td>
            <td data-label="状态"><span class="badge ${record.status === "BORROWED" ? "success" : "muted"}">${record.status === "BORROWED" ? "借阅中" : "已归还"}</span></td>
            <td class="actions">
                ${record.status === "BORROWED" ? `<button class="btn secondary" onclick="returnBook(${record.id})" type="button">归还</button>` : ""}
            </td>
        </tr>
    `).join("");
}

async function saveCategory(event) {
    event.preventDefault();
    await withButton(event.submitter, "保存中", async () => {
        const form = new FormData(event.target);
        await api("/api/categories", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(form.entries()))
        });
        event.target.reset();
        await refreshCategories();
        showMessage("分类已新增");
    });
}

async function editCategory(id) {
    const category = state.categories.find((item) => item.id === id);
    if (!category) return;
    openFormDialog({
        eyebrow: "Category",
        title: "编辑分类",
        summary: "修改分类名称和描述后，相关图书会继续归属到该分类。",
        submitText: "保存分类",
        fields: [
            { name: "name", label: "分类名称", value: category.name, required: true },
            { name: "description", label: "分类描述", value: category.description, required: true, full: true }
        ],
        onSubmit: async (payload) => {
            await api(`/api/categories/${id}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            await refreshCategories();
            showMessage("分类已更新");
        }
    });
}

async function deleteCategory(id) {
    const category = state.categories.find((item) => item.id === id);
    openConfirmDialog({
        title: "删除分类",
        message: `确认删除“${category?.name ?? "该分类"}”？如果分类下仍有图书，系统会阻止删除。`,
        confirmText: "删除分类",
        onConfirm: async () => {
            await api(`/api/categories/${id}`, { method: "DELETE" });
            await refreshCategories();
            showMessage("分类已删除");
        }
    });
}

async function viewCategoryBooks(id) {
    await run(async () => {
        const category = state.categories.find((item) => item.id === id);
        const books = (await loadAllBooks()).filter((book) => book.categoryId === id);
        openCategoryBooksDialog(category, books);
    });
}

function openCategoryBooksDialog(category, books) {
    const dialog = document.querySelector("#categoryBooksDialog");
    const title = document.querySelector("#categoryBooksTitle");
    const summary = document.querySelector("#categoryBooksSummary");
    const body = document.querySelector("#categoryBookRows");

    title.textContent = `${category?.name ?? "分类"} 下的图书`;
    summary.textContent = books.length > 0 ? `共 ${books.length} 本图书` : "该分类下暂无图书";
    body.innerHTML = books.length === 0 ? emptyRow(6, "该分类下暂无图书") : books.map((book) => `
        <tr>
            <td data-label="ISBN"><code>${escapeHtml(book.isbn)}</code></td>
            <td data-label="书名">${escapeHtml(book.title)}</td>
            <td data-label="作者">${escapeHtml(book.author)}</td>
            <td data-label="出版社">${escapeHtml(book.publisher)}</td>
            <td data-label="库存"><span class="badge ${book.availableCopies > 0 ? "success" : "muted"}">${book.availableCopies}/${book.totalCopies}</span></td>
            <td data-label="状态">${formatBookStatus(book.status)}</td>
        </tr>
    `).join("");

    dialog.hidden = false;
    dialog.querySelector(".dialog-close").focus();
}

function closeCategoryBooksDialog() {
    document.querySelector("#categoryBooksDialog").hidden = true;
}

async function saveBook(event) {
    event.preventDefault();
    await withButton(event.submitter, "保存中", async () => {
        const form = new FormData(event.target);
        const payload = Object.fromEntries(form.entries());
        payload.totalCopies = Number(payload.totalCopies);
        payload.availableCopies = payload.availableCopies === "" ? null : Number(payload.availableCopies);
        payload.categoryId = Number(payload.categoryId);
        await api("/api/books", { method: "POST", body: JSON.stringify(payload) });
        event.target.reset();
        await refreshBooks();
        showMessage("图书已新增");
    });
}

async function editBook(id) {
    const book = state.books.find((item) => item.id === id);
    if (!book) return;
    openFormDialog({
        eyebrow: "Book",
        title: "编辑图书",
        summary: "可同步调整基础信息和库存，系统会保留已有借阅记录。",
        submitText: "保存图书",
        fields: [
            { name: "isbn", label: "ISBN", value: book.isbn, required: true },
            { name: "title", label: "书名", value: book.title, required: true },
            { name: "author", label: "作者", value: book.author, required: true },
            { name: "publisher", label: "出版社", value: book.publisher, required: true },
            { name: "totalCopies", label: "馆藏", type: "number", min: 0, value: book.totalCopies, required: true },
            { name: "availableCopies", label: "可借", type: "number", min: 0, value: book.availableCopies },
            { name: "categoryId", label: "分类", type: "select", value: book.categoryId, required: true, options: state.categories.map((category) => ({ value: category.id, label: category.name })) }
        ],
        onSubmit: async (payload) => {
            const totalCopies = Number(payload.totalCopies);
            const availableCopies = payload.availableCopies === "" ? Math.min(book.availableCopies, totalCopies) : Number(payload.availableCopies);
            await api(`/api/books/${id}`, {
                method: "PUT",
                body: JSON.stringify({
                    isbn: payload.isbn,
                    title: payload.title,
                    author: payload.author,
                    publisher: payload.publisher,
                    totalCopies,
                    availableCopies: Math.min(availableCopies, totalCopies),
                    categoryId: Number(payload.categoryId),
                    status: book.status
                })
            });
            await refreshBooks();
            showMessage("图书已更新");
        }
    });
}

async function deleteBook(id) {
    const book = state.books.find((item) => item.id === id);
    openConfirmDialog({
        title: "删除图书",
        message: `确认删除《${book?.title ?? "该图书"}》？已有借阅记录时系统会阻止删除。`,
        confirmText: "删除图书",
        onConfirm: async () => {
            await api(`/api/books/${id}`, { method: "DELETE" });
            await refreshBooks();
            showMessage("图书已删除");
        }
    });
}

async function borrowBook(bookId) {
    await run(async () => {
        await api("/api/borrows", {
            method: "POST",
            body: JSON.stringify({ bookId, days: 14 })
        });
        await refreshBooks();
        await refreshBorrows(false);
        showMessage("借阅成功");
    });
}

async function returnBook(id) {
    await run(async () => {
        await api(`/api/borrows/${id}/return`, { method: "POST" });
        await refreshBooks();
        await refreshBorrows(false);
        showMessage("归还成功");
    });
}

async function askAi() {
    await withButton(document.querySelector("#askAiBtn"), "生成中", async () => {
        const question = document.querySelector("#aiQuestion").value;
        const answerNode = document.querySelector("#aiAnswer");
        answerNode.textContent = "正在结合馆藏数据生成推荐...";
        answerNode.classList.add("loading");
        
        try {
            const response = await fetch("/api/ai/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ question })
            });
            if (response.redirected) {
                window.location.href = response.url;
                return;
            }
            if (!response.ok) {
                throw new Error("请求失败，HTTP " + response.status);
            }
            answerNode.textContent = "";
            answerNode.classList.remove("loading");
            let fullText = "";
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = "";
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                const parts = buffer.split("\n\n");
                buffer = parts.pop();
                for (let part of parts) {
                    let text = part.split("\n").map(l => l.startsWith("data:") ? l.substring(5) : l).join("\n");
                    fullText += text;
                    answerNode.textContent = fullText;
                }
            }
        } catch (error) {
            answerNode.classList.remove("loading");
            answerNode.textContent += "\n大模型生成异常：" + error.message;
        }
    });
}

async function refreshAiModels() {
    if (!isCurrentAdmin()) {
        return;
    }
    state.aiModels = await api("/api/ai/models");
    renderAiModels();
}

async function refreshAiSettings() {
    if (!isCurrentAdmin()) {
        return;
    }
    state.aiSettings = await api("/api/ai/settings");
    renderAiSettings();
}

function renderAiSettings() {
    const status = document.querySelector("#aiSettingsStatus");
    const provider = document.querySelector("#settingProviderSelect")?.value || "MiMo";
    let configured = false;
    let mask = "";
    if (provider === "DeepSeek") {
        configured = state.aiSettings?.deepseekApiKeyConfigured;
        mask = state.aiSettings?.deepseekApiKeyMask;
    } else {
        configured = state.aiSettings?.apiKeyConfigured;
        mask = state.aiSettings?.apiKeyMask;
    }

    status.classList.remove("error");
    if (configured) {
        status.textContent = `${provider} API Key 已配置：${mask}`;
        status.classList.add("success");
        return;
    }
    status.textContent = `${provider} API Key 未配置`;
    status.classList.remove("success");
}

async function saveAiSettings(event) {
    event.preventDefault();
    const status = document.querySelector("#aiSettingsStatus");
    const form = new FormData(event.target);
    const payload = Object.fromEntries(form.entries());

    if (!payload.provider || !payload.provider.trim()) {
        status.textContent = "保存失败：服务商不能为空";
        status.classList.remove("success");
        status.classList.add("error");
        return;
    }
    if (!payload.apiKey || !payload.apiKey.trim()) {
        status.textContent = "保存失败：API Key 不能为空";
        status.classList.remove("success");
        status.classList.add("error");
        return;
    }

    const button = event.submitter;
    const originalBtnHtml = button.innerHTML;
    button.disabled = true;
    button.innerHTML = "保存中...";
    status.textContent = "正在保存...";
    status.classList.remove("success", "error");

    try {
        await api("/api/ai/settings", {
            method: "POST",
            body: JSON.stringify({ provider: payload.provider.trim(), apiKey: payload.apiKey.trim() })
        });
        event.target.elements.apiKey.value = "";
        await refreshAiSettings();
    } catch (error) {
        status.textContent = "保存失败：" + (error.message || "请求异常");
        status.classList.remove("success");
        status.classList.add("error");
    } finally {
        button.disabled = false;
        button.innerHTML = originalBtnHtml;
    }
}

function renderAiModels() {
    const rows = document.querySelector("#aiModelRows");
    if (state.aiModels.length === 0) {
        rows.innerHTML = emptyRow(4, "暂无可用模型");
        return;
    }
    rows.innerHTML = state.aiModels.map((model) => `
        <tr>
            <td data-label="模型">
                <code>${escapeHtml(model.modelName)}</code>
                ${isActiveAiModel(model.id) ? `<span class="badge success">当前使用</span>` : ""}
            </td>
            <td data-label="服务商">${escapeHtml(model.provider)}</td>
            <td data-label="添加时间">${formatTime(model.createdAt)}</td>
            <td class="actions">
                <button class="btn secondary" onclick="activateAiModel(${model.id})" type="button" ${isActiveAiModel(model.id) ? "disabled" : ""}>设为使用</button>
                <button class="btn danger" onclick="deleteAiModel(${model.id})" type="button">删除</button>
            </td>
        </tr>
    `).join("");
}

async function saveAiModel(event) {
    event.preventDefault();
    const form = new FormData(event.target);
    const payload = Object.fromEntries(form.entries());
    await withButton(event.submitter, "测试中", async () => {
        await api("/api/ai/models", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        event.target.reset();
        const provider = event.target.elements.provider.value;
        if (provider === "DeepSeek") {
            event.target.elements.modelName.value = "deepseek-chat";
        } else {
            event.target.elements.modelName.value = "mimo-v2.5-pro";
        }
        await refreshAiModels();
        await refreshAiSettings();
        showMessage("模型测试成功，已添加");
    });
}

async function activateAiModel(id) {
    await run(async () => {
        await api(`/api/ai/models/${id}/activate`, { method: "POST" });
        await refreshAiSettings();
        renderAiModels();
        showMessage("已切换全局使用模型");
    });
}

async function deleteAiModel(id) {
    const model = state.aiModels.find((item) => item.id === id);
    openConfirmDialog({
        title: "删除模型",
        message: `确认删除模型“${model?.modelName ?? "该模型"}”？删除后系统会使用列表中的其他模型或默认配置。`,
        confirmText: "删除模型",
        onConfirm: async () => {
            await api(`/api/ai/models/${id}`, { method: "DELETE" });
            await refreshAiSettings();
            await refreshAiModels();
            showMessage("模型已删除");
        }
    });
}

function openFormDialog(config) {
    const dialog = document.querySelector("#formDialog");
    const form = document.querySelector("#formDialogForm");
    const title = document.querySelector("#formDialogTitle");
    const eyebrow = document.querySelector("#formDialogEyebrow");
    const summary = document.querySelector("#formDialogSummary");

    state.lastFocusedElement = document.activeElement;
    state.activeDialog = dialog;
    eyebrow.textContent = config.eyebrow;
    title.textContent = config.title;
    summary.textContent = config.summary || "";
    form.innerHTML = [
        ...config.fields.map(renderDialogField),
        `<div class="dialog-actions field full">
            <button class="btn secondary" type="button" data-dialog-cancel>取消</button>
            <button class="btn primary" type="submit">${escapeHtml(config.submitText || "保存")}</button>
        </div>`
    ].join("");

    form.onsubmit = async (event) => {
        event.preventDefault();
        clearDialogError(form);
        const payload = Object.fromEntries(new FormData(form).entries());
        if (!validateDialogPayload(form, config.fields, payload)) {
            return;
        }
        await withButton(event.submitter, "保存中", async () => {
            await config.onSubmit(payload);
            closeFormDialog();
        });
    };
    form.querySelector("[data-dialog-cancel]").addEventListener("click", closeFormDialog);
    dialog.hidden = false;
    form.querySelector("input, select, textarea, button")?.focus();
}

function closeFormDialog() {
    const dialog = document.querySelector("#formDialog");
    dialog.hidden = true;
    document.querySelector("#formDialogForm").onsubmit = null;
    restoreDialogFocus(dialog);
}

function renderDialogField(field) {
    const fullClass = field.full ? " full" : "";
    const required = field.required ? " required" : "";
    const min = field.min !== undefined ? ` min="${escapeHtml(field.min)}"` : "";
    const value = field.value ?? "";

    if (field.type === "select") {
        const options = (field.options || []).map((option) => (
            `<option value="${escapeHtml(option.value)}" ${String(option.value) === String(value) ? "selected" : ""}>${escapeHtml(option.label)}</option>`
        )).join("");
        return `<label class="field${fullClass}"><span>${escapeHtml(field.label)}</span><select name="${escapeHtml(field.name)}"${required}>${options}</select></label>`;
    }

    if (field.type === "textarea") {
        return `<label class="field${fullClass}"><span>${escapeHtml(field.label)}</span><textarea name="${escapeHtml(field.name)}"${required}>${escapeHtml(value)}</textarea></label>`;
    }

    return `<label class="field${fullClass}"><span>${escapeHtml(field.label)}</span><input name="${escapeHtml(field.name)}" type="${escapeHtml(field.type || "text")}" value="${escapeHtml(value)}"${min}${required}></label>`;
}

function validateDialogPayload(form, fields, payload) {
    const invalid = fields.find((field) => {
        if (field.required && String(payload[field.name] ?? "").trim() === "") {
            return true;
        }
        if (field.type === "number" && payload[field.name] !== "" && Number.isNaN(Number(payload[field.name]))) {
            return true;
        }
        return false;
    });

    if (!invalid) {
        return true;
    }

    const error = document.createElement("p");
    error.className = "field-error field full";
    error.textContent = `请正确填写${invalid.label}`;
    form.prepend(error);
    form.elements[invalid.name]?.focus();
    return false;
}

function clearDialogError(form) {
    form.querySelector(".field-error")?.remove();
}

function openConfirmDialog(config) {
    const dialog = document.querySelector("#confirmDialog");
    const title = document.querySelector("#confirmDialogTitle");
    const message = document.querySelector("#confirmDialogMessage");
    const confirmButton = document.querySelector("#confirmDialogConfirm");

    state.lastFocusedElement = document.activeElement;
    state.activeDialog = dialog;
    title.textContent = config.title;
    message.textContent = config.message;
    confirmButton.textContent = config.confirmText || "确认";
    confirmButton.onclick = async () => {
        await withButton(confirmButton, "处理中", async () => {
            await config.onConfirm();
            closeConfirmDialog();
        });
    };
    dialog.hidden = false;
    confirmButton.focus();
}

function closeConfirmDialog() {
    const dialog = document.querySelector("#confirmDialog");
    dialog.hidden = true;
    document.querySelector("#confirmDialogConfirm").onclick = null;
    restoreDialogFocus(dialog);
}

function restoreDialogFocus(dialog) {
    if (state.activeDialog === dialog) {
        state.activeDialog = null;
    }
    if (state.lastFocusedElement && typeof state.lastFocusedElement.focus === "function") {
        state.lastFocusedElement.focus();
    }
    state.lastFocusedElement = null;
}

function isActiveAiModel(id) {
    return Number(state.aiSettings?.activeModelId) === Number(id);
}

async function withButton(button, loadingText, task) {
    const original = button?.innerHTML;
    try {
        if (button) {
            button.disabled = true;
            button.innerHTML = loadingText;
        }
        await run(task);
    } finally {
        if (button) {
            button.disabled = false;
            button.innerHTML = original;
        }
    }
}

async function run(task) {
    try {
        await task();
    } catch (error) {
        showMessage(error.message || "操作失败，请检查输入后重试");
    }
}

function updateMetrics() {
    document.querySelector("#bookCount").textContent = state.books.length;
    document.querySelector("#availableCount").textContent = state.books.reduce((sum, book) => sum + book.availableCopies, 0);
    document.querySelector("#categoryCount").textContent = state.categories.length;
}

function showMessage(text) {
    message.hidden = false;
    message.textContent = text;
    window.clearTimeout(showMessage.timer);
    showMessage.timer = window.setTimeout(() => {
        message.hidden = true;
    }, 3600);
}

function emptyRow(colspan, text) {
    return `<tr><td class="row-empty" colspan="${colspan}">${text}</td></tr>`;
}

function formatTime(value) {
    return value ? value.replace("T", " ").slice(0, 16) : "";
}

function formatBookStatus(value) {
    if (value === "ON_SHELF") {
        return "在架";
    }
    if (value === "OFF_SHELF") {
        return "下架";
    }
    return escapeHtml(value);
}

function isCurrentAdmin() {
    return state.me?.roles?.includes("ROLE_ADMIN");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
