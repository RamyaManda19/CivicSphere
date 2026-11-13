/**
 * CivicSphere Recurring Civic Issue Management & Resolution Accountability
 */
const Recurrence = {

  async init() {
    const user = await Auth.requireAuth('ADMIN');
    if (!user) return;

    await this.loadStats();
    await this.loadRecurrenceRecords();
  },

  async loadStats() {
    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const res = await fetch(`${base}/api/recurrence/stats`, { credentials: 'include' });
      if (!res.ok) return;
      const json = await res.json();
      const stats = json.data || {};

      document.getElementById('recTotal').textContent = stats.TOTAL || 0;
      document.getElementById('recPending').textContent = stats.PENDING || 0;
      document.getElementById('recConfirmed').textContent = stats.CONFIRMED || 0;
      document.getElementById('recRejected').textContent = stats.REJECTED || 0;
    } catch (e) {
      console.error('Error loading recurrence stats:', e);
    }
  },

  async loadRecurrenceRecords() {
    const base = window.Auth ? Auth.baseUrl : '';
    const status = document.getElementById('filterVerification')?.value || '';
    const container = document.getElementById('recurrenceCardsContainer');
    if (!container) return;

    try {
      const url = `${base}/api/recurrence?status=${encodeURIComponent(status)}`;
      const res = await fetch(url, { credentials: 'include' });
      const json = await res.json();
      const list = json.data || [];

      if (list.length === 0) {
        container.innerHTML = `
          <div class="card" style="text-align:center; padding:3rem; color:#64748b;">
            <h3>No recurring civic issues found</h3>
            <p style="margin-top:0.5rem;">The recurrence detection engine continuously compares incoming complaints against resolved issues.</p>
          </div>
        `;
        return;
      }

      container.innerHTML = list.map(r => {
        const orig = r.originalComplaint || {};
        const rec = r.recurringComplaint || {};
        const score = r.matchingScore || 0;

        let statusBadge = '<span class="badge badge-under-review">Pending Verification</span>';
        if (r.verificationStatus === 'CONFIRMED') {
          statusBadge = '<span class="badge badge-recurrence">⚠️ Confirmed Recurring</span>';
        } else if (r.verificationStatus === 'REJECTED') {
          statusBadge = '<span class="badge badge-closed">Rejected Match</span>';
        }

        return `
          <div class="card" style="margin-bottom:1.5rem; border-left: 5px solid ${score >= 70 ? '#ef4444' : '#f59e0b'};">
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.5rem; margin-bottom:1rem;">
              <div>
                <span style="font-weight:700; font-size:1.1rem; color:#0f172a;">Recurrence Alert #${r.recurrenceId}</span>
                <span style="font-size:0.85rem; color:#64748b; margin-left:0.5rem;">Detected: ${r.createdAt || ''}</span>
              </div>
              <div style="display:flex; align-items:center; gap:0.75rem;">
                <span class="score-badge">${score}% Match</span>
                ${statusBadge}
              </div>
            </div>

            <div style="background:#f8fafc; padding:0.75rem 1rem; border-radius:6px; font-size:0.88rem; margin-bottom:1rem; border:1px solid #e2e8f0;">
              <strong>🔍 Detection Engine Rationale:</strong> ${escapeHtml(r.matchingReason)}
            </div>

            <div class="compare-container">
              <!-- Original Resolved Complaint -->
              <div class="compare-col">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.5rem;">
                  <h4 style="margin:0; border:none; padding:0;">Original Resolved Issue</h4>
                  <span class="badge badge-resolved">#${orig.complaintId || r.originalComplaintId}</span>
                </div>
                <div style="font-weight:600; font-size:1rem; color:#0f172a; margin-bottom:0.35rem;">
                  ${escapeHtml(orig.title || 'Original Complaint')}
                </div>
                <div style="font-size:0.85rem; color:#64748b; margin-bottom:0.3rem;">
                  <strong>Category:</strong> ${escapeHtml(orig.categoryName || 'N/A')}
                </div>
                <div style="font-size:0.85rem; color:#64748b; margin-bottom:0.3rem;">
                  <strong>Location:</strong> ${escapeHtml(orig.area || '')}, ${escapeHtml(orig.city || '')}
                </div>
                <div style="font-size:0.85rem; color:#065f46; background:#ecfdf5; padding:0.35rem 0.5rem; border-radius:4px; margin-top:0.5rem;">
                  <strong>Previous Status:</strong> ${orig.status || 'RESOLVED'} (Resolved: ${orig.resolvedAt ? orig.resolvedAt.split(' ')[0] : 'N/A'})
                </div>
              </div>

              <!-- New Recurring Complaint -->
              <div class="compare-col">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.5rem;">
                  <h4 style="margin:0; border:none; padding:0;">New Recurring Report</h4>
                  <span class="badge badge-submitted">#${rec.complaintId || r.recurringComplaintId}</span>
                </div>
                <div style="font-weight:600; font-size:1rem; color:#0f172a; margin-bottom:0.35rem;">
                  ${escapeHtml(rec.title || 'New Complaint')}
                </div>
                <div style="font-size:0.85rem; color:#64748b; margin-bottom:0.3rem;">
                  <strong>Reported By:</strong> ${escapeHtml(rec.userName || 'Citizen')}
                </div>
                <div style="font-size:0.85rem; color:#64748b; margin-bottom:0.3rem;">
                  <strong>Location:</strong> ${escapeHtml(rec.area || '')}, ${escapeHtml(rec.city || '')}
                </div>
                <div style="font-size:0.85rem; color:#1e40af; background:#eff6ff; padding:0.35rem 0.5rem; border-radius:4px; margin-top:0.5rem;">
                  <strong>Current Status:</strong> ${rec.status || 'SUBMITTED'} (Submitted: ${rec.submittedAt ? rec.submittedAt.split(' ')[0] : 'N/A'})
                </div>
              </div>
            </div>

            <!-- Action Buttons for Verification -->
            <div style="display:flex; justify-content:flex-end; gap:0.75rem; margin-top:1.25rem; border-top:1px solid #f1f5f9; padding-top:1rem;">
              <a href="${base}/citizen/complaint-details.html?id=${r.recurringComplaintId}" target="_blank" class="btn btn-secondary btn-sm">Inspect Full Details ↗</a>
              ${r.verificationStatus === 'PENDING' ? `
                <button onclick="Recurrence.verify(${r.recurrenceId}, 'CONFIRMED')" class="btn btn-danger btn-sm">⚠️ Confirm Recurrence</button>
                <button onclick="Recurrence.verify(${r.recurrenceId}, 'REJECTED')" class="btn btn-secondary btn-sm">Dismiss / Reject Match</button>
              ` : `
                <span style="font-size:0.85rem; color:#64748b; align-self:center;">Verified by ${escapeHtml(r.verifiedByName || 'Admin')} on ${r.verifiedAt || ''}</span>
              `}
            </div>
          </div>
        `;
      }).join('');

    } catch (e) {
      container.innerHTML = `<div class="card" style="color:red; text-align:center;">Failed to load recurrence records: ${e.message}</div>`;
    }
  },

  async verify(recurrenceId, status) {
    const actionLabel = status === 'CONFIRMED' ? 'confirm this issue as recurring' : 'reject this recurrence match';
    if (!confirm(`Are you sure you want to ${actionLabel}?`)) return;

    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const res = await fetch(`${base}/api/recurrence/${recurrenceId}/verify`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ verificationStatus: status })
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.error || 'Verification failed');

      showToast(`Recurrence ${status.toLowerCase()} successfully!`, 'success');
      await Recurrence.loadStats();
      await Recurrence.loadRecurrenceRecords();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
};
