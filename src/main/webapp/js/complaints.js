/**
 * CivicSphere Complaints Handling, Submission & Tracking
 */
const Complaints = {

  async loadCategories(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const res = await fetch(`${base}/api/categories`);
      const json = await res.json();
      const categories = json.data || [];
      select.innerHTML = '<option value="">-- Select Issue Category --</option>' +
        categories.map(cat => `<option value="${cat.categoryId}">${cat.categoryName}</option>`).join('');
    } catch (e) {
      console.error('Failed to load categories', e);
    }
  },

  async loadDepartments(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const res = await fetch(`${base}/api/departments`);
      const json = await res.json();
      const departments = json.data || [];
      select.innerHTML = '<option value="">-- Select Department --</option>' +
        departments.map(d => `<option value="${d.departmentId}">${d.departmentName}</option>`).join('');
    } catch (e) {
      console.error('Failed to load departments', e);
    }
  },

  detectLocation() {
    if (!navigator.geolocation) {
      showToast('Geolocation is not supported by your browser.', 'error');
      return;
    }
    const btn = document.getElementById('btnDetectLoc');
    if (btn) btn.textContent = 'Locating GPS...';

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        document.getElementById('latitude').value = pos.coords.latitude.toFixed(6);
        document.getElementById('longitude').value = pos.coords.longitude.toFixed(6);
        if (btn) btn.textContent = '📍 GPS Detected';
        showToast('Coordinates detected successfully!', 'success');
      },
      (err) => {
        if (btn) btn.textContent = '📍 Detect Current GPS';
        showToast('Unable to fetch GPS: ' + err.message, 'error');
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  },

  handleImagePreview(input, previewId) {
    const preview = document.getElementById(previewId);
    if (!preview || !input.files || !input.files[0]) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      preview.src = e.target.result;
      preview.style.display = 'block';
    };
    reader.readAsDataURL(input.files[0]);
  },

  async submitComplaint(form) {
    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Submitting & Analyzing...';
    }

    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const formData = new FormData(form);
      const res = await fetch(`${base}/api/complaints`, {
        method: 'POST',
        credentials: 'include',
        body: formData
      });

      const result = await res.json();
      if (!res.ok || !result.success) {
        throw new Error(result.error || 'Submission failed');
      }

      showToast('Complaint submitted successfully!', 'success');
      setTimeout(() => {
        window.location.href = `${base}/citizen/my-complaints.html`;
      }, 1200);

    } catch (e) {
      showToast(e.message, 'error');
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Complaint';
      }
    }
  },

  async loadMyComplaints() {
    const tableBody = document.getElementById('myComplaintsTable');
    if (!tableBody) return;

    try {
      const base = window.Auth ? Auth.baseUrl : '';
      const res = await fetch(`${base}/api/complaints/my`, { credentials: 'include' });
      const json = await res.json();
      const list = json.data || [];

      if (list.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2.5rem; color:#64748b;">No complaints submitted yet.</td></tr>`;
        return;
      }

      tableBody.innerHTML = list.map(c => {
        const badgeClass = 'badge-' + (c.status || '').toLowerCase().replace(/\s+/g, '-');
        const recBadge = c.isRecurring ? `<span class="badge badge-recurrence">⚠️ Recurring Issue</span>` : '';
        const dateStr = c.submittedAt ? c.submittedAt.split(' ')[0] : 'N/A';

        return `
          <tr>
            <td><strong>#${c.complaintId}</strong></td>
            <td>
              <div style="font-weight:600; color:#0f172a;">${escapeHtml(c.title)}</div>
              ${recBadge}
            </td>
            <td>${escapeHtml(c.categoryName || 'General')}</td>
            <td>${escapeHtml(c.area)}, ${escapeHtml(c.city)}</td>
            <td>${dateStr}</td>
            <td><span class="badge ${badgeClass}">${c.status}</span></td>
            <td>
              <a href="${base}/citizen/complaint-details.html?id=${c.complaintId}" class="btn btn-sm btn-primary">Track Details</a>
            </td>
          </tr>
        `;
      }).join('');

    } catch (e) {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:red;">Failed to load complaints.</td></tr>`;
    }
  },

  async loadComplaintDetails() {
    const base = window.Auth ? Auth.baseUrl : '';
    const params = new URLSearchParams(window.location.search);
    const complaintId = params.get('id');
    if (!complaintId) {
      window.location.href = `${base}/citizen/my-complaints.html`;
      return;
    }

    try {
      const res = await fetch(`${base}/api/complaints/${complaintId}`, { credentials: 'include' });
      const json = await res.json();
      if (!res.ok || !json.success) {
        showToast('Complaint not found', 'error');
        return;
      }

      const c = json.data.complaint;
      const history = json.data.history || [];
      const recurrence = json.data.recurrence;

      // Populate basic info
      document.getElementById('detailTitle').textContent = `#${c.complaintId} - ${c.title}`;
      document.getElementById('detailCategory').textContent = c.categoryName || 'N/A';
      document.getElementById('detailDepartment').textContent = c.departmentName || 'Awaiting Assignment';
      document.getElementById('detailLocation').textContent = `${c.area}, ${c.city}, ${c.state}${c.landmark ? ' (Near ' + c.landmark + ')' : ''}`;
      document.getElementById('detailDescription').textContent = c.description;
      document.getElementById('detailSubmittedAt').textContent = c.submittedAt || 'N/A';

      const statusBadge = document.getElementById('detailStatus');
      const badgeClass = 'badge-' + (c.status || '').toLowerCase().replace(/\s+/g, '-');
      statusBadge.className = `badge ${badgeClass}`;
      statusBadge.textContent = c.status;

      // Evidence photo
      const imgContainer = document.getElementById('evidenceImageContainer');
      if (imgContainer) {
        if (c.imagePath) {
          imgContainer.innerHTML = `<img src="${base}/uploads/${c.imagePath}" alt="Evidence" style="max-width:100%; max-height:280px; border-radius:8px; border:1px solid #e2e8f0; object-fit:cover;">`;
        } else {
          imgContainer.innerHTML = `<p style="color:#94a3b8; font-style:italic;">No evidence photograph attached.</p>`;
        }
      }

      // Resolution info
      const resBox = document.getElementById('resolutionInfoBox');
      if (resBox) {
        if (c.status === 'RESOLVED' || c.status === 'CLOSED') {
          resBox.style.display = 'block';
          document.getElementById('resRemarks').textContent = c.resolutionRemarks || 'Verified and resolved.';
          document.getElementById('resDate').textContent = c.resolvedAt || 'N/A';
        } else {
          resBox.style.display = 'none';
        }
      }

      // Recurrence alert box
      const recBox = document.getElementById('recurrenceAlertBox');
      if (recBox) {
        if (c.isRecurring || recurrence) {
          recBox.style.display = 'block';
          const score = recurrence ? recurrence.matchingScore : (c.recurrenceScore || 85);
          const reason = recurrence ? recurrence.matchingReason : 'Identified recurring pattern with past resolved complaint.';
          document.getElementById('recScore').textContent = `${score}% Match Score`;
          document.getElementById('recReason').textContent = reason;
        } else {
          recBox.style.display = 'none';
        }
      }

      // Render Timeline
      const timelineEl = document.getElementById('timelineContainer');
      if (timelineEl) {
        if (history.length === 0) {
          timelineEl.innerHTML = '<p style="color:#64748b;">No history records available.</p>';
        } else {
          timelineEl.innerHTML = history.map(h => `
            <div class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="timeline-title">${h.newStatus}</span>
                  <span class="timeline-date">${h.updatedAt}</span>
                </div>
                <div class="timeline-remarks">${escapeHtml(h.remarks || 'Status update')}</div>
                <div class="timeline-author">Updated by: ${h.updatedByName || 'System'} (${h.updatedByRole || 'Admin'})</div>
              </div>
            </div>
          `).join('');
        }
      }

      // Recurrence reporting button for citizen (if resolved)
      const reportRecBtn = document.getElementById('btnReportRecurrence');
      if (reportRecBtn) {
        if (c.status === 'RESOLVED' || c.status === 'CLOSED') {
          reportRecBtn.style.display = 'inline-flex';
          reportRecBtn.onclick = () => {
            window.location.href = `${base}/citizen/submit-complaint.html?recurrenceOf=${c.complaintId}`;
          };
        } else {
          reportRecBtn.style.display = 'none';
        }
      }

    } catch (e) {
      showToast('Error loading complaint: ' + e.message, 'error');
    }
  }
};
