const authSlider = document.querySelector(".auth-slider");
const loginTab = document.querySelector("#loginTab");
const registerTab = document.querySelector("#registerTab");
const loginForm = document.querySelector("#loginForm");
const registerForm = document.querySelector("#registerForm");
const authTitle = document.querySelector("#authTitle");
const authMessage = document.querySelector("#authMessage");
const captchaButtons = document.querySelectorAll("[data-captcha-refresh]");

loginTab.addEventListener("click", () => switchAuthMode("login"));
registerTab.addEventListener("click", () => switchAuthMode("register"));
registerForm.addEventListener("submit", registerAccount);
captchaButtons.forEach((button) => {
    button.addEventListener("click", () => refreshCaptcha(button.dataset.targetForm));
});

const initialMode = window.location.pathname === "/register" ? "register" : "login";
switchAuthMode(initialMode);

if (window.location.search.includes("error=captcha")) {
    showAuthMessage("验证码错误，请重新输入", true);
} else if (window.location.search.includes("error")) {
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
    refreshCaptcha(isLogin ? "loginForm" : "registerForm");
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
        refreshCaptcha("registerForm");
    } finally {
        submitButton.disabled = false;
        submitButton.innerHTML = originalText;
    }
}

async function refreshCaptcha(formId) {
    const form = document.querySelector(`#${formId}`);
    const button = form?.querySelector("[data-captcha-refresh]");
    const input = form?.elements.captcha;
    if (!form || !button || !input) {
        return;
    }

    button.disabled = true;
    button.textContent = "加载中";
    try {
        const response = await fetch("/api/captcha", { cache: "no-store" });
        const result = await response.json();
        button.textContent = result.question || "刷新";
        input.value = "";
    } catch (error) {
        button.textContent = "刷新验证码";
        showAuthMessage("验证码加载失败，请点击刷新", true);
    } finally {
        button.disabled = false;
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
