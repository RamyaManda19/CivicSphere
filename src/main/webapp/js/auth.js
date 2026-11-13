/**
 * CivicSphere Authentication & Session Manager
 */
const Auth = {
  baseUrl: window.location.pathname.startsWith('/CivicSphere') ? '/CivicSphere' : '',

  async getMe() {
    try {
      const res = await fetch(`${this.baseUrl}/api/auth/me`, {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        return data.data;
      }
      return null;
    } catch (e) {
      console.error('Auth check error:', e);
      return null;
    }
  },

  async requireAuth(requiredRole = null) {
    const user = await this.getMe();
    if (!user) {
      window.location.href = `${this.baseUrl}/login.html`;
      return null;
    }
    if (requiredRole && user.role.toUpperCase() !== requiredRole.toUpperCase()) {
      if (user.role.toUpperCase() === 'ADMIN') {
        window.location.href = `${this.baseUrl}/admin/dashboard.html`;
      } else {
        window.location.href = `${this.baseUrl}/citizen/dashboard.html`;
      }
      return null;
    }
    this.renderUserBadge(user);
    return user;
  },

  async login(email, password) {
    const res = await fetch(`${this.baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password })
    });
    const result = await res.json();
    if (!res.ok || !result.success) {
      throw new Error(result.error || 'Login failed');
    }
    return result.data;
  },

  async register(formData) {
    const res = await fetch(`${this.baseUrl}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(formData)
    });
    const result = await res.json();
    if (!res.ok || !result.success) {
      throw new Error(result.error || 'Registration failed');
    }
    return result.data;
  },

  async logout() {
    try {
      await fetch(`${this.baseUrl}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      });
    } finally {
      window.location.href = `${this.baseUrl}/login.html`;
    }
  },

  renderUserBadge(user) {
    const badge = document.getElementById('userBadgeContainer');
    if (badge && user) {
      badge.innerHTML = `
        <div class="user-badge">
          <span><strong>${user.fullName}</strong></span>
          <span class="role-tag">${user.role}</span>
          <button onclick="Auth.logout()" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.75rem; margin-left: 0.4rem;">Logout</button>
        </div>
      `;
    }
  }
};

/**
 * Toast notifications
 */
function showToast(message, type = 'success') {
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}
