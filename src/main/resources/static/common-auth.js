function getToken(){
    return localStorage.getItem("jwt");
}

function logout(){
    localStorage.removeItem("jwt");
    window.location.href = "login.html";
}

function requireAuth(){
    if (!getToken()) {
        window.location.href = "login.html";
    }
}