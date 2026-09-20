const storedUserId = sessionStorage.getItem("userId");
let CURRENT_INSPECTOR_ID = storedUserId ? Number.parseInt(storedUserId, 10) : null;
let CURRENT_INSPECTOR_NAME = sessionStorage.getItem("username") || "Inspector";

let activeTaskId = null;
let selectedFiles = [];
let lastParsedReport = null;
let sessionReports = [];
let currentProductBase64Images = []; 

const imageInput = document.getElementById('imageInput');
const previewContainer = document.getElementById('previewContainer');
const submitBtn = document.getElementById('submitBtn');
const finalSubmitBtn = document.getElementById('finalSubmitBtn');

document.addEventListener('DOMContentLoaded', () => {

  if (!Number.isInteger(CURRENT_INSPECTOR_ID) || CURRENT_INSPECTOR_ID <= 0) {
    alert("No active login session found. Redirecting to login...");
    window.location.href = '/index.html';
    return;
  }


  const welcomeEl = document.getElementById('welcomeInspectorNameDashboard');
  if (welcomeEl) {
    welcomeEl.textContent = CURRENT_INSPECTOR_NAME;
  }
  const welcomeElAudit = document.getElementById('welcomeInspectorNameAudit');
  if (welcomeElAudit) welcomeElAudit.textContent = CURRENT_INSPECTOR_NAME;

  loadAssignedTasks();
  setupImageUploadListeners();
  setupFinalSubmitListener();
});

function handleLogout() {
  sessionStorage.clear();
  window.location.href = '/index.html';
}

async function loadAssignedTasks() {
  const taskListEl = document.getElementById('taskList');
  if (!taskListEl) return;

  if (!CURRENT_INSPECTOR_ID) return;

  try {
    const res = await fetch(`/api/admin/tasks/inspector/${CURRENT_INSPECTOR_ID}`);
    const tasks = await res.json();

    if (!tasks || tasks.length === 0) {
      taskListEl.innerHTML = '<p style="color: #64748b;">No active tasks assigned at this moment.</p>';
      return;
    }

    taskListEl.innerHTML = '';
    tasks.forEach(task => {
      const card = document.createElement('div');
      card.className = 'task-card';

      if (task.status === "REQUESTED") {
        card.innerHTML = `
          <div class="task-info">
            <h3>${task.locationName}</h3>
            <p style="color: #b45309; font-weight: 600;">Pending Task Request</p>
            <span class="risk-badge">Priority Risk Score: ${task.riskScore ? task.riskScore.toFixed(2) : 'N/A'}</span>
          </div>
          <div style="display: flex; gap: 8px; margin-top: 8px;">
            <button class="start-btn" style="background: #16a34a;" onclick="respondToTaskRequest(${task.id}, 'accept')">Accept</button>
            <button class="start-btn" style="background: #dc2626;" onclick="respondToTaskRequest(${task.id}, 'decline')">Decline</button>
          </div>
        `;
      } else {
        card.innerHTML = `
          <div class="task-info">
            <h3>${task.locationName}</h3>
            <p>Assigned At: ${new Date(task.assignedAt).toLocaleString()}</p>
            <span class="risk-badge">Priority Risk Score: ${task.riskScore ? task.riskScore.toFixed(2) : 'N/A'}</span>
          </div>
          <button class="start-btn" onclick="openUploadPanel(${task.id}, '${escapeHtml(task.locationName)}', ${task.riskScore})">
            Start Audit
          </button>
        `;
      }
      taskListEl.appendChild(card);
    });

  } catch (err) {
    console.error('Error fetching assigned tasks:', err);
    taskListEl.innerHTML = '<p style="color: #ef4444;">Failed to load assigned tasks from server.</p>';
  }
}

async function respondToTaskRequest(taskId, action) {
  try {
    const response = await fetch(`/api/admin/tasks/${action}/${taskId}`, { method: 'POST' });
    if (response.ok) {
      alert(`Task request ${action}ed successfully!`);
      loadAssignedTasks();
    } else {
      alert(`Failed to ${action} task request.`);
    }
  } catch (err) {
    console.error(`Error responding to task request (${action}):`, err);
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

async function openUploadPanel(taskId, locationName, riskScore) {
  activeTaskId = taskId;
  document.getElementById('currentTaskTitle').textContent = locationName;
  document.getElementById('currentTaskMeta').textContent = `Task ID: #${taskId} | Priority Risk Score: ${riskScore ? riskScore.toFixed(2) : 'N/A'}`;
  
  resetFullSessionState();

  document.getElementById('taskDashboardView').classList.add('hidden');
  document.getElementById('uploadPanelView').classList.remove('hidden');


  try {
    const res = await fetch(`/api/admin/tasks/all`);
    const allTasks = await res.json();
    const currentTask = allTasks.find(t => t.id === taskId);

    if (currentTask && currentTask.latitude && currentTask.longitude) {
      initInspectorRouteMap(currentTask.latitude, currentTask.longitude, locationName);
    } else {
      initInspectorRouteMap(19.076, 72.8777, locationName);
    }
  } catch (e) {
    console.warn("Could not fetch destination coordinates for task route, displaying default center:", e);
    initInspectorRouteMap(19.076, 72.8777, locationName);
  }
}

function openAdhocAuditPanel() {
  activeTaskId = "adhoc";
  document.getElementById('currentTaskTitle').textContent = "Self-Initiated On-Site Audit";
  document.getElementById('currentTaskMeta').textContent = "Ad-Hoc Inspection | Unassigned Venue";

  resetFullSessionState();

  document.getElementById('taskDashboardView').classList.add('hidden');
  document.getElementById('uploadPanelView').classList.remove('hidden');
}

function showDashboard() {
  activeTaskId = null;
  resetFullSessionState();

  document.getElementById('uploadPanelView').classList.add('hidden');
  document.getElementById('taskDashboardView').classList.remove('hidden');
  
  loadAssignedTasks();
}

function resetFullSessionState() {
  selectedFiles = [];
  lastParsedReport = null;
  sessionReports = [];
  currentProductBase64Images = [];
  updateQueueCountUI();

  if (previewContainer) previewContainer.innerHTML = '';
  if (imageInput) imageInput.value = '';
  if (submitBtn) submitBtn.disabled = true;
  
  const reportContainer = document.getElementById('reportContainer');
  if (reportContainer) reportContainer.classList.add('hidden');
}

function resetSingleUploadCard() {
  selectedFiles = [];
  currentProductBase64Images = [];
  if (previewContainer) previewContainer.innerHTML = '';
  if (imageInput) imageInput.value = '';
  if (submitBtn) submitBtn.disabled = true;
}

function saveAndAuditNextProduct() {
  if (lastParsedReport) {
    sessionReports.push(lastParsedReport);
    lastParsedReport = null;
  }

  resetSingleUploadCard();
  document.getElementById('reportContainer').classList.add('hidden');
  
  updateQueueCountUI();
  alert(`Product saved! Total products queued for this site: ${sessionReports.length}. You can now upload photos for the next product.`);
}

function updateQueueCountUI() {
  const currentTotal = sessionReports.length + (lastParsedReport ? 1 : 0);
  const queueCountEl = document.getElementById('queueCount');
  const productBadgeEl = document.getElementById('productBadge');
  
  if (queueCountEl) queueCountEl.textContent = Math.max(1, currentTotal);
  if (productBadgeEl) productBadgeEl.textContent = `Product #${currentTotal || 1}`;
}


function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = (error) => reject(error);
  });
}

function setupImageUploadListeners() {
  if (!imageInput) return;

  imageInput.addEventListener('change', function(event) {
    const files = Array.from(event.target.files);

    files.forEach(file => {
      selectedFiles.push(file);
      const imageUrl = URL.createObjectURL(file);

      const card = document.createElement('div');
      card.classList.add('preview-card');
      card.innerHTML = `
        <img src="${imageUrl}" alt="Preview">
        <button class="remove-btn" type="button">&times;</button>
      `;

      card.querySelector('.remove-btn').addEventListener('click', () => {
        selectedFiles = selectedFiles.filter(f => f !== file);
        URL.revokeObjectURL(imageUrl);
        card.remove();
        if (selectedFiles.length === 0) {
          submitBtn.disabled = true;
        }
      });

      previewContainer.appendChild(card);
    });

    if (selectedFiles.length > 0) submitBtn.disabled = false;
    imageInput.value = '';
  });

  submitBtn.addEventListener('click', async () => {
    if (selectedFiles.length === 0) return;


    currentProductBase64Images = [];
    for (const file of selectedFiles) {
      try {
        const b64 = await fileToBase64(file);
        currentProductBase64Images.push(b64);
      } catch (e) {
        console.warn('Could not encode file to Base64:', e);
      }
    }

    const formData = new FormData();
    selectedFiles.forEach(file => formData.append('images', file));
    if (activeTaskId) formData.append('taskId', activeTaskId);

    submitBtn.disabled = true;
    submitBtn.textContent = 'Analyzing Images...';
    submitBtn.style.backgroundColor = '#94a3b8'; 
    submitBtn.style.cursor = 'not-allowed';

    try {
      const response = await fetch('/api/image/upload', { method: 'POST', body: formData });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Server returned ${response.status}: ${errorText}`);
      }

      const rawText = await response.text();
      let parsedData;
      
      try {
        parsedData = JSON.parse(rawText);
        if (typeof parsedData === 'string') parsedData = JSON.parse(parsedData);
      } catch (e) {
        console.warn('Direct parse fallback');
      }

      let extractedReport = null;
      if (parsedData?.candidates?.[0]?.content?.parts?.[0]?.text) {
        extractedReport = JSON.parse(parsedData.candidates[0].content.parts[0].text);
      } else if (parsedData && typeof parsedData === 'object') {
        extractedReport = parsedData;
      }

      if (extractedReport) {

        extractedReport.productImages = currentProductBase64Images;

        extractedReport.productImage = currentProductBase64Images[0] || null;

        renderComplianceReport(extractedReport);
      } else {
        alert('Upload completed, could not parse structured report.');
      }

    } catch (error) {
      console.error('Upload failed:', error);
      alert('Upload failed: ' + error.message);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Upload Images';
    }
  });
}

function renderComplianceReport(report) {
  lastParsedReport = report;
  updateQueueCountUI();

  const reportContainer = document.getElementById('reportContainer');
  const statusBadge = document.getElementById('statusBadge');
  const attributeTableBody = document.getElementById('attributeTableBody');
  const missingSection = document.getElementById('missingSection');
  const missingList = document.getElementById('missingList');

  if (report.is_compliant) {
    statusBadge.textContent = 'Compliant';
    statusBadge.className = 'status-badge pass';
    missingSection.classList.add('hidden');
  } else {
    statusBadge.textContent = 'Non-Compliant';
    statusBadge.className = 'status-badge fail';
    missingSection.classList.remove('hidden');
  }

  const fields = [
    { key: 'mrp', label: 'Maximum Retail Price (MRP)', rule: 'Rule 6(1)(e)' },
    { key: 'net_quantity', label: 'Net Quantity', rule: 'Rule 6(1)(c) & 13' },
    { key: 'month_year', label: 'Manufacturing/Import Date', rule: 'Rule 6(1)(d)' },
    { key: 'generic_name', label: 'Generic/Common Name', rule: 'Rule 6(1)(b)' },
    { key: 'manufacturer_details', label: 'Manufacturer & Address', rule: 'Rule 6(1)(a) & 10' },
    { key: 'consumer_care', label: 'Consumer Care Contact', rule: 'Rule 6(2)' }
  ];

  attributeTableBody.innerHTML = '';
  fields.forEach(field => {
    const item = report[field.key];
    const val = item ? item.value : null;
    const isPresent = val !== null && val !== undefined && val !== '';

    const row = document.createElement('tr');
    row.innerHTML = `
      <td>
        <strong>${field.label}</strong><br>
        <small style="color: #64748b;">${field.rule}</small>
      </td>
      <td class="${isPresent ? 'val-present' : 'val-missing'}">
        ${isPresent ? val : 'Not Detected'}
      </td>
      <td>
        <span style="color: ${isPresent ? '#16a34a' : '#dc2626'}; font-weight: bold;">
          ${isPresent ? '✓ Valid' : '✕ Violation'}
        </span>
      </td>
    `;
    attributeTableBody.appendChild(row);
    reportContainer.classList.remove('hidden');


  if (inspectorMap) {
    setTimeout(() => {
      inspectorMap.invalidateSize();
    }, 150);
  }
  });

  missingList.innerHTML = '';
  if (report.violations && report.violations.length > 0) {
    report.violations.forEach(violation => {
      const li = document.createElement('li');
      li.textContent = violation;
      missingList.appendChild(li);
    });
  }

  reportContainer.classList.remove('hidden');

  if (inspectorMap) {
    setTimeout(() => inspectorMap.invalidateSize(), 0);
  }
}

function setupFinalSubmitListener() {
  if (!finalSubmitBtn) return;

  finalSubmitBtn.addEventListener('click', async () => {
    const payloadArray = [...sessionReports];
    if (lastParsedReport) {
      payloadArray.push(lastParsedReport);
    }

    if (payloadArray.length === 0) {
      alert("Please upload and analyze at least one package photo before submitting.");
      return;
    }

    finalSubmitBtn.disabled = true;
    finalSubmitBtn.textContent = 'Submitting All Reports...';

    const endpointId = activeTaskId ? activeTaskId : "adhoc";

    try {
      const response = await fetch(`/api/admin/tasks/complete/${endpointId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          inspectorId: CURRENT_INSPECTOR_ID,
          reportData: JSON.stringify(payloadArray)
        })
      });

      if (!response.ok) {
        throw new Error('Failed to submit inspection reports.');
      }

      alert(`Successfully submitted ${payloadArray.length} product audit report(s) to the Legal Metrology Department!`);
      showDashboard();

    } catch (error) {
      console.error('Submission error:', error);
      alert('Error submitting reports: ' + error.message);
    } finally {
      finalSubmitBtn.disabled = false;
      updateQueueCountUI();
    }
  });
}

let inspectorMap = null;
let routingControl = null;


function initInspectorRouteMap(targetLat, targetLng, locationName) {
  const mapContainer = document.getElementById("inspectorMap");
  if (!mapContainer) return;


  const currentLat = parseFloat(sessionStorage.getItem("userLat")) || 19.076;
  const currentLng = parseFloat(sessionStorage.getItem("userLng")) || 72.8777;

 
  if (!inspectorMap) {
    inspectorMap = L.map("inspectorMap").setView([targetLat || currentLat, targetLng || currentLng], 13);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap contributors"
    }).addTo(inspectorMap);
  } else {
 
    setTimeout(() => inspectorMap.invalidateSize(), 200);
  }

  
  if (routingControl) {
    inspectorMap.removeControl(routingControl);
    routingControl = null;
  }

  // Define custom icons
  const officerIcon = L.icon({
    iconUrl: "https://cdn-icons-png.flaticon.com/512/3135/3135715.png",
    iconSize: [36, 36],
    iconAnchor: [18, 36]
  });

  const targetStoreIcon = L.icon({
    iconUrl: "https://cdn-icons-png.flaticon.com/512/684/684908.png",
    iconSize: [36, 36],
    iconAnchor: [18, 36]
  });

  
  if (targetLat && targetLng) {
    routingControl = L.Routing.control({
      waypoints: [
        L.latLng(currentLat, currentLng),
        L.latLng(targetLat, targetLng)
      ],
      lineOptions: {
        styles: [{ color: "#2563eb", weight: 6, opacity: 0.8 }]
      },
      createMarker: function(i, wp) {
        if (i === 0) {
          return L.marker(wp.latLng, { icon: officerIcon }).bindPopup("<b>Your Location</b>");
        } else {
          return L.marker(wp.latLng, { icon: targetStoreIcon }).bindPopup(`<b>Destination:</b> ${locationName}`);
        }
      },
      show: false, 
      addWaypoints: false,
      draggableWaypoints: false
    }).addTo(inspectorMap);
  } else {

    L.marker([currentLat, currentLng], { icon: officerIcon }).addTo(inspectorMap)
      .bindPopup("<b>Your Current Position</b>").openPopup();
  }
}