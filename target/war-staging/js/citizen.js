/**
 * CivicSphere Citizen Dashboard Manager
 */
const Citizen = {
  async initDashboard() {
    const user = await Auth.requireAuth('CITIZEN');
    if (!user) return;

    const welcomeEl = document.getElementById('citizenGreeting');
    if (welcomeEl) {
      welcomeEl.textContent = `Welcome back, ${user.fullName}`;
    }

    await this.loadStats();
    await this.loadRecentComplaints();
  },

  async loadStats() {
    try {
      const res = await fetch(`${Auth.baseUrl}/api/complaints/my`, { credentials: 'include' });
      if (!res.ok) return;
      const json = await res.json();
      const list = json.data || [];

      const total = list.length;
      const pending = list.filter(c => c.status === 'SUBMITTED' || c.status === 'UNDER REVIEW').length;
      const inProgress = list.filter(c => c.status === 'ASSIGNED' || c.status === 'IN PROGRESS').length;
      const resolved = list.filter(c => c.status === 'RESOLVED' || c.status === 'CLOSED').length;
      const recurring = list.filter(c => c.isRecurring).length;

      document.getElementById('statTotal').textContent = total;
      document.getElementById('statPending').textContent = pending;
      document.getElementById('statInProgress').textContent = inProgress;
      document.getElementById('statResolved').textContent = resolved;
      if (document.getElementById('statRecurring')) {
        document.getElementById('statRecurring').textContent = recurring;
      }
    } catch (e) {
      console.error('Error loading citizen stats:', e);
    }
  },

  async loadRecentComplaints() {
    const tableBody = document.getElementById('recentComplaintsTable');
    if (!tableBody) return;

    try {
      const res = await fetch(`${Auth.baseUrl}/api/complaints/my`, { credentials: 'include' });
      const json = await res.json();
      const list = json.data || [];

      if (list.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 2rem; color: #64748b;">No complaints submitted yet. Click "Report Issue" to submit your first complaint!</td></tr>`;
        return;
      }

      const recent = list.slice(0, 5);
      tableBody.innerHTML = recent.map(c => {
        const badgeClass = 'badge-' + (c.status || '').toLowerCase().replace(/\s+/g, '-');
        const recBadge = c.isRecurring ? `<span class="badge badge-recurrence">⚠️ Recurring</span>` : '';
        return `
          <tr>
            <td><strong>#${c.complaintId}</strong></td>
            <td>
              <a href="${Auth.baseUrl}/citizen/complaint-details.html?id=${c.complaintId}" style="font-weight:600;">${escapeHtml(c.title)}</a>
              ${recBadge}
            </td>
            <td>${escapeHtml(c.categoryName || 'General')}</td>
            <td>${escapeHtml(c.area)}, ${escapeHtml(c.city)}</td>
            <td><span class="badge ${badgeClass}">${c.status}</span></td>
            <td>
              <a href="${Auth.baseUrl}/citizen/complaint-details.html?id=${c.complaintId}" class="btn btn-sm btn-secondary">Track</a>
            </td>
          </tr>
        `;
      }).join('');
    } catch (e) {
      tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: red;">Failed to load complaints.</td></tr>`;
    }
  }
};

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
