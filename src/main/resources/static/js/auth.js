const authSlider = document.querySelector(".auth-slider");
const loginTab = document.querySelector("#loginTab");
const registerTab = document.querySelector("#registerTab");
const loginForm = document.querySelector("#loginForm");
const registerForm = document.querySelector("#registerForm");
const authTitle = document.querySelector("#authTitle");
const authMessage = document.querySelector("#authMessage");

loginTab.addEventListener("click", () => switchAuthMode("login"));
registerTab.addEventListener("click", () => switchAuthMode("register"));
registerForm.addEventListener("submit", registerAccount);

if (window.location.pathname === "/register") {
    switchAuthMode("register");
}

if (window.location.search.includes("error")) {
    showAuthMessage("用户名或密码错误，请重试", true);
}
if (window.location.search.includes("logout")) {
    showAuthMessage("您已成功退出登录");
}

function switchAuthMode(mode) {
    const isLogin = mode === "login";
    authSlider.classList.toggle("register-mode", !isLogin);
    loginTab.classList.toggle("active", isLogin);
    registerTab.classList.toggle("active", !isLogin);
    loginTab.setAttribute("aria-selected", String(isLogin));
    registerTab.setAttribute("aria-selected", String(!isLogin));
    loginForm.hidden = !isLogin;
    registerForm.hidden = isLogin;
    authTitle.textContent = isLogin ? "登录系统" : "注册读者账号";
    hideAuthMessage();
    (isLogin ? loginForm : registerForm).querySelector("input")?.focus();
}

async function registerAccount(event) {
    event.preventDefault();
    const submitButton = event.submitter;
    const originalText = submitButton.innerHTML;
    const payload = Object.fromEntries(new FormData(registerForm).entries());

    if (payload.password !== payload.confirmPassword) {
        showAuthMessage("两次输入的密码不一致", true);
        registerForm.elements.confirmPassword.focus();
        return;
    }

    try {
        submitButton.disabled = true;
        submitButton.textContent = "注册中";
        const response = await fetch("/api/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.message || "注册失败，请检查输入");
        }
        registerForm.reset();
        loginForm.reset();
        switchAuthMode("login");
        showAuthMessage(result.message || "注册成功，请登录");
        loginForm.elements.username.focus();
    } catch (error) {
        showAuthMessage(error.message, true);
    } finally {
        submitButton.disabled = false;
        submitButton.innerHTML = originalText;
    }
}

function showAuthMessage(text, isError = false) {
    authMessage.hidden = false;
    authMessage.textContent = text;
    authMessage.classList.toggle("error", isError);
}

function hideAuthMessage() {
    authMessage.hidden = true;
    authMessage.textContent = "";
    authMessage.classList.remove("error");
}
