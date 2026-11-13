/**
 * CivicSphere Administrator Portal & Analytics Manager
 */
const Admin = {
  categoryChart: null,
  deptChart: null,

  async initDashboard() {
    const user = await Auth.requireAuth('ADMIN');
    if (!user) return;

    await this.loadDashboardData();
  },

  async loadDashboardData() {
    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const res = await fetch(`${base}/api/admin/dashboard`, { credentials: 'include' });
      if (!res.ok) return;
      const json = await res.json();
      const stats = json.data;

      const sc = stats.statusCounts || {};
      const rc = stats.recurrenceStats || {};

      document.getElementById('statTotal').textContent = sc.TOTAL || 0;
      document.getElementById('statPending').textContent = (sc.SUBMITTED || 0) + (sc.UNDER_REVIEW || 0);
      document.getElementById('statInProgress').textContent = (sc.ASSIGNED || 0) + (sc.IN_PROGRESS || 0);
      document.getElementById('statResolved').textContent = (sc.RESOLVED || 0) + (sc.CLOSED || 0);
      document.getElementById('statRecurring').textContent = rc.TOTAL || 0;
      document.getElementById('statPendingRec').textContent = rc.PENDING || 0;

      // Render Charts
      this.renderCategoryChart(stats.categoryCounts || {});
      this.renderDeptChart(stats.departmentCounts || {});

      // Render Recent Activity
      this.renderRecentActivity(stats.recentActivity || []);

    } catch (e) {
      console.error('Error loading admin dashboard stats:', e);
    }
  },

  renderCategoryChart(dataMap) {
    const ctx = document.getElementById('categoryChart');
    if (!ctx) return;

    const labels = Object.keys(dataMap);
    const data = Object.values(dataMap);

    if (this.categoryChart) this.categoryChart.destroy();

    this.categoryChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: [
            '#0d9488', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#10b981'
          ]
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'bottom' }
        }
      }
    });
  },

  renderDeptChart(dataMap) {
    const ctx = document.getElementById('departmentChart');
    if (!ctx) return;

    const labels = Object.keys(dataMap);
    const data = Object.values(dataMap);

    if (this.deptChart) this.deptChart.destroy();

    this.deptChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Complaints Handled',
          data: data,
          backgroundColor: '#0284c7'
        }]
      },
      options: {
        responsive: true,
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  },

  renderRecentActivity(list) {
    const tbody = document.getElementById('recentActivityTable');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 2rem;">No recent activity.</td></tr>';
      return;
    }

    tbody.innerHTML = list.map(c => {
      const badgeClass = 'badge-' + (c.status || '').toLowerCase().replace(/\s+/g, '-');
      const recBadge = c.isRecurring ? `<span class="badge badge-recurrence">⚠️ Recurring</span>` : '';
      return `
        <tr>
          <td><strong>#${c.complaintId}</strong></td>
          <td>
            <strong>${escapeHtml(c.title)}</strong> ${recBadge}
            <div style="font-size:0.78rem; color:#64748b;">By: ${escapeHtml(c.userName || 'Citizen')}</div>
          </td>
          <td>${escapeHtml(c.categoryName || 'General')}</td>
          <td>${escapeHtml(c.area)}, ${escapeHtml(c.city)}</td>
          <td><span class="badge ${badgeClass}">${c.status}</span></td>
          <td>
            <a href="/admin/complaints.html?id=${c.complaintId}" class="btn btn-sm btn-secondary">Manage</a>
          </td>
        </tr>
      `;
    }).join('');
  },

  async loadAdminComplaints() {
    const base = window.Auth ? Auth.baseUrl : '';
    const status = document.getElementById('filterStatus')?.value || '';
    const categoryId = document.getElementById('filterCategory')?.value || '';
    const search = document.getElementById('filterSearch')?.value || '';

    const tbody = document.getElementById('adminComplaintsTable');
    if (!tbody) return;

    try {
      const url = `${base}/api/admin/complaints?status=${encodeURIComponent(status)}&categoryId=${encodeURIComponent(categoryId)}&search=${encodeURIComponent(search)}`;
      const res = await fetch(url, { credentials: 'include' });
      const json = await res.json();
      const list = json.data || [];

      if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding: 2rem; color:#64748b;">No complaints matching criteria.</td></tr>';
        return;
      }

      tbody.innerHTML = list.map(c => {
        const badgeClass = 'badge-' + (c.status || '').toLowerCase().replace(/\s+/g, '-');
        const recBadge = c.isRecurring ? `<span class="badge badge-recurrence">⚠️ Recurring</span>` : '';
        const dateStr = c.submittedAt ? c.submittedAt.split(' ')[0] : 'N/A';

        return `
          <tr>
            <td><strong>#${c.complaintId}</strong></td>
            <td>
              <div style="font-weight:600; color:#0f172a;">${escapeHtml(c.title)}</div>
              <div style="font-size:0.78rem; color:#64748b;">Reported by: ${escapeHtml(c.userName || 'Citizen')} (${escapeHtml(c.userPhone || '')})</div>
              ${recBadge}
            </td>
            <td>${escapeHtml(c.categoryName || 'General')}</td>
            <td>${escapeHtml(c.departmentName || 'Unassigned')}</td>
            <td>${escapeHtml(c.area)}, ${escapeHtml(c.city)}</td>
            <td>${dateStr}</td>
            <td><span class="badge ${badgeClass}">${c.status}</span></td>
            <td>
              <div style="display:flex; gap:0.35rem; flex-wrap:wrap;">
                <button onclick="Admin.openAssignModal(${c.complaintId}, '${escapeHtml(c.title)}')" class="btn btn-sm btn-secondary" title="Assign Dept">Assign</button>
                <button onclick="Admin.openStatusModal(${c.complaintId}, '${c.status}')" class="btn btn-sm btn-primary" title="Update Status">Status</button>
                ${c.status !== 'RESOLVED' && c.status !== 'CLOSED' ? `
                  <button onclick="Admin.openResolveModal(${c.complaintId})" class="btn btn-sm btn-success" title="Mark Resolved">Resolve</button>
                ` : ''}
              </div>
            </td>
          </tr>
        `;
      }).join('');

    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:red;">Failed to load complaints.</td></tr>';
    }
  },

  openAssignModal(complaintId, title) {
    document.getElementById('assignComplaintId').value = complaintId;
    document.getElementById('assignModalTitle').textContent = `Assign Complaint #${complaintId}`;
    Complaints.loadDepartments('assignDeptSelect');
    openModal('assignModal');
  },

  async handleAssignSubmit(e) {
    e.preventDefault();
    const base = window.Auth ? Auth.baseUrl : '';
    const id = document.getElementById('assignComplaintId').value;
    const deptId = document.getElementById('assignDeptSelect').value;
    const remarks = document.getElementById('assignRemarks').value;

    try {
      const res = await fetch(`${base}/api/admin/complaints/${id}/assign`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ departmentId: parseInt(deptId), remarks })
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.error || 'Assignment failed');

      showToast('Department assigned successfully!', 'success');
      closeModal('assignModal');
      Admin.loadAdminComplaints();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  openStatusModal(complaintId, currentStatus) {
    document.getElementById('statusComplaintId').value = complaintId;
    document.getElementById('newStatusSelect').value = currentStatus;
    document.getElementById('statusRemarks').value = '';
    openModal('statusModal');
  },

  async handleStatusSubmit(e) {
    e.preventDefault();
    const base = window.Auth ? Auth.baseUrl : '';
    const id = document.getElementById('statusComplaintId').value;
    const status = document.getElementById('newStatusSelect').value;
    const remarks = document.getElementById('statusRemarks').value;

    try {
      const res = await fetch(`${base}/api/complaints/${id}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ status, remarks })
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.error || 'Status update failed');

      showToast('Status updated successfully!', 'success');
      closeModal('statusModal');
      Admin.loadAdminComplaints();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  openResolveModal(complaintId) {
    document.getElementById('resolveComplaintId').value = complaintId;
    document.getElementById('resolveRemarks').value = '';
    openModal('resolveModal');
  },

  async handleResolveSubmit(e) {
    e.preventDefault();
    const base = window.Auth ? Auth.baseUrl : '';
    const id = document.getElementById('resolveComplaintId').value;
    const remarks = document.getElementById('resolveRemarks').value;

    try {
      const res = await fetch(`${base}/api/complaints/${id}/resolve`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ remarks })
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.error || 'Resolution failed');

      showToast('Complaint verified and marked as resolved!', 'success');
      closeModal('resolveModal');
      Admin.loadAdminComplaints();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }
};

function openModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.add('active');
}

function closeModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.remove('active');
}
