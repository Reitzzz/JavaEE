const state = {
    me: null,
    categories: [],
    books: [],
    loadingButton: null
};

const message = document.querySelector("#message");

document.addEventListener("DOMContentLoaded", async () => {
    bindNavigation();
    bindActions();
    await run(async () => {
        await loadMe();
        await refreshCategories();
        await refreshBooks();
        await refreshBorrows(false);
    });
});

function bindNavigation() {
    document.querySelectorAll(".nav-item").forEach((item) => {
        item.addEventListener("click", () => {
            document.querySelectorAll(".nav-item, .view").forEach((node) => node.classList.remove("active"));
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
    document.querySelector("#loadMineBtn").addEventListener("click", () => refreshBorrows(false));
    document.querySelector("#loadAllBorrowsBtn").addEventListener("click", () => refreshBorrows(true));
    document.querySelector("#askAiBtn").addEventListener("click", askAi);
    document.querySelector("#categoryForm").addEventListener("submit", saveCategory);
    document.querySelector("#bookForm").addEventListener("submit", saveBook);
    document.querySelector("#categoryBooksDialog").addEventListener("click", (event) => {
        if (event.target.id === "categoryBooksDialog") {
            closeCategoryBooksDialog();
        }
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !document.querySelector("#categoryBooksDialog").hidden) {
            closeCategoryBooksDialog();
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
            <td>${category.id}</td>
            <td>${escapeHtml(category.name)}</td>
            <td>${escapeHtml(category.description)}</td>
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
            <td><code>${escapeHtml(book.isbn)}</code></td>
            <td>${escapeHtml(book.title)}</td>
            <td>${escapeHtml(book.author)}</td>
            <td>${escapeHtml(book.categoryName)}</td>
            <td><span class="badge ${book.availableCopies > 0 ? "success" : "muted"}">${book.availableCopies}/${book.totalCopies}</span></td>
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
            <td>${escapeHtml(record.displayName)}</td>
            <td>${escapeHtml(record.bookTitle)}</td>
            <td>${formatTime(record.borrowedAt)}</td>
            <td>${formatTime(record.dueAt)}</td>
            <td><span class="badge ${record.status === "BORROWED" ? "success" : "muted"}">${record.status === "BORROWED" ? "借阅中" : "已归还"}</span></td>
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
    const name = prompt("分类名称", category.name);
    if (!name) return;
    const description = prompt("分类描述", category.description);
    if (!description) return;
    await run(async () => {
        await api(`/api/categories/${id}`, {
            method: "PUT",
            body: JSON.stringify({ name, description })
        });
        await refreshCategories();
        showMessage("分类已更新");
    });
}

async function deleteCategory(id) {
    if (!confirm("确认删除该分类？如果分类下仍有图书，系统会阻止删除。")) return;
    await run(async () => {
        await api(`/api/categories/${id}`, { method: "DELETE" });
        await refreshCategories();
        showMessage("分类已删除");
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
            <td><code>${escapeHtml(book.isbn)}</code></td>
            <td>${escapeHtml(book.title)}</td>
            <td>${escapeHtml(book.author)}</td>
            <td>${escapeHtml(book.publisher)}</td>
            <td><span class="badge ${book.availableCopies > 0 ? "success" : "muted"}">${book.availableCopies}/${book.totalCopies}</span></td>
            <td>${formatBookStatus(book.status)}</td>
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
    const title = prompt("书名", book.title);
    if (!title) return;
    const totalCopies = Number(prompt("馆藏总数", book.totalCopies));
    if (Number.isNaN(totalCopies)) return;
    await run(async () => {
        await api(`/api/books/${id}`, {
            method: "PUT",
            body: JSON.stringify({
                isbn: book.isbn,
                title,
                author: book.author,
                publisher: book.publisher,
                totalCopies,
                availableCopies: Math.min(book.availableCopies, totalCopies),
                categoryId: book.categoryId,
                status: book.status
            })
        });
        await refreshBooks();
        showMessage("图书已更新");
    });
}

async function deleteBook(id) {
    if (!confirm("确认删除该图书？已有借阅记录时系统会阻止删除。")) return;
    await run(async () => {
        await api(`/api/books/${id}`, { method: "DELETE" });
        await refreshBooks();
        showMessage("图书已删除");
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
        document.querySelector("#aiAnswer").textContent = "正在结合馆藏数据生成推荐...";
        const result = await api("/api/ai/chat", {
            method: "POST",
            body: JSON.stringify({ question })
        });
        document.querySelector("#aiAnswer").textContent = result.answer;
    });
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
