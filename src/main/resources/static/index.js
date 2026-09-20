async function handleLogin(event) {
  if (event) event.preventDefault();
  const usernameInput = document.getElementById("username").value;
  const passwordInput = document.getElementById("password").value;
  const roleInput = document.getElementById("role").value;

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: usernameInput,
        password: passwordInput,
        role: roleInput,
      }),
    });

    if (response.ok) {
      let data = {};
      const responseText = await response.text();
      try {
        data = JSON.parse(responseText);
      } catch (e) {
        data = { message: responseText };
      }

      sessionStorage.setItem("userRole", roleInput);
      sessionStorage.setItem("username", usernameInput);
      const loggedInUserId = data.userId ?? data.id;
      if (loggedInUserId !== undefined && loggedInUserId !== null) {
        sessionStorage.setItem("userId", String(loggedInUserId));
      }

      alert("Success: " + (data.message || "Login successful"));

      if (roleInput === "admin") {
        window.location.href = '/admin.html';
      } else if (roleInput === "inspector") {
        window.location.href = '/inspector.html';
      }
    } else {
      const errorText = await response.text();
      alert("Error: " + errorText);
    }
  } catch (err) {
    console.error("Login failed:", err);
    alert("Login failed. Please check your backend connection.");
  }
}