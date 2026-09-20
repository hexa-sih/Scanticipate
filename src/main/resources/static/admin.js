let completedTasksCache = [];
let activeReportList = [];
let selectedCoords = null;
let inspectorsList = [];
let storeLayerGroup = L.layerGroup();
let heatLayerGroup = null;

document.addEventListener("DOMContentLoaded", () => {
  const username = sessionStorage.getItem("username") || "Admin";
  document.getElementById("welcomeAdminName").textContent = username;
});

function handleLogout() {
  sessionStorage.clear();
  window.location.href = "/index.html";
}

const map = L.map("map").setView([19.076, 72.8777], 12);
storeLayerGroup.addTo(map);

L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

const inspectorIcon = L.icon({
  iconUrl: "https://cdn-icons-png.flaticon.com/512/3135/3135715.png",
  iconSize: [36, 36],
  iconAnchor: [18, 36],
  popupAnchor: [0, -32],
});

const storeIcon = L.divIcon({
  className: "custom-store-pin",
  html: `<div style="background-color: #ef4444; width: 14px; height: 14px; border-radius: 50%; border: 2px solid white; box-shadow: 0 0 6px rgba(0,0,0,0.4);"></div>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

function loadHeatmapData() {
  fetch("/api/admin/heatmap-data")
    .then((res) => res.json())
    .then((heatPoints) => {
      if (typeof heatPoints === "string") {
        heatPoints = JSON.parse(heatPoints);
      }

      if (Array.isArray(heatPoints) && heatPoints.length > 0) {
        const formattedPoints = heatPoints
          .map((pt) => {
            if (Array.isArray(pt)) {
              return [pt[0], pt[1], pt[2] !== undefined ? pt[2] : 0.5];
            } else if (typeof pt === "object" && pt !== null) {
              const lat = pt.latitude || pt.lat || 0;
              const lng = pt.longitude || pt.lng || 0;
              const risk =
                pt.predicted_risk ||
                pt.riskScore ||
                pt.baselineRiskScore ||
                0.5;
              return [lat, lng, risk];
            }
            return null;
          })
          .filter((pt) => pt && pt[0] !== 0 && pt[1] !== 0);

        const highRiskHotspots = formattedPoints.filter(
          (pt) => pt[2] >= 0.85,
        );

        document.getElementById("zoneCount").textContent =
          `${highRiskHotspots.length} Hotspots`;

        if (heatLayerGroup) {
          map.removeLayer(heatLayerGroup);
        }

        heatLayerGroup = L.heatLayer(formattedPoints, {
          radius: 30,
          blur: 18,
          maxZoom: 16,
          max: 1.0,
          gradient: {
            0.3: "#3b82f6",
            0.6: "#eab308",
            0.85: "#ef4444",
          },
        }).addTo(map);
      } else {
        document.getElementById("zoneCount").textContent = "0 Hotspots";
      }
    })
    .catch((err) => {
      console.error("Error loading heatmap data:", err);
      document.getElementById("zoneCount").textContent = "0 Hotspots";
    });
}

async function loadInspectors() {
  try {
    const res = await fetch("/api/admin/inspectors");
    inspectorsList = await res.json();

    document.getElementById("inspectorCount").textContent =
      `${inspectorsList.length} Online`;

    const select = document.getElementById("inspectorSelect");
    select.innerHTML = '<option value="">-- Choose Inspector --</option>';

    inspectorsList.forEach((inspector) => {
      if (inspector.latitude && inspector.longitude) {
        const marker = L.marker(
          [inspector.latitude, inspector.longitude],
          { icon: inspectorIcon },
        ).addTo(map);
        marker.bindPopup(`
          <b>${inspector.username}</b><br>
          Status: <span style="color: ${inspector.status === "AVAILABLE" ? "green" : "orange"};">${inspector.status || "AVAILABLE"}</span>
        `);
      }

      const option = document.createElement("option");
      option.value = inspector.id;
      option.textContent = `${inspector.username} (${inspector.status || "AVAILABLE"})`;
      select.appendChild(option);
    });
  } catch (err) {
    console.error("Failed to load inspectors:", err);
  }
}

async function loadStoreMarkers() {
  try {
    const response = await fetch("/api/admin/stores");
    const stores = await response.json();

    if (!stores || stores.length === 0) return;

    storeLayerGroup.clearLayers();

    stores.forEach((store) => {
      if (store.latitude && store.longitude) {
        const marker = L.marker([store.latitude, store.longitude], {
          icon: storeIcon,
        });

        const hoverTooltipContent = `
          <div style="font-family: sans-serif; padding: 2px;">
            <strong style="font-size: 0.9rem; color: #0f172a;">${store.name}</strong><br>
            <small style="color: #64748b;">Category: ${store.category || "Retail Store"}</small><br>
            <span style="color: #dc2626; font-weight: bold;">Past Violations: ${store.totalViolationsCount || 0}</span><br>
            <span style="color: #2563eb; font-weight: bold;">Risk Score: ${(store.baselineRiskScore || 0.5).toFixed(2)}</span>
          </div>
        `;
        marker.bindTooltip(hoverTooltipContent, {
          direction: "top",
          offset: [0, -8],
        });

        marker.on("click", function (e) {
          L.DomEvent.stopPropagation(e);

          selectedCoords = { lat: store.latitude, lng: store.longitude };
          document.getElementById("locationName").value = store.name;
          document.getElementById("riskScore").value = (
            store.baselineRiskScore || 0.5
          ).toFixed(2);

          populateInspectorsWithDistance(store.latitude, store.longitude);
          document.getElementById("taskModal").classList.add("active");
        });

        storeLayerGroup.addLayer(marker);
      }
    });
  } catch (err) {
    console.error("Failed to load store markers from database:", err);
  }
}

async function loadCompletedTasks() {
  const container = document.getElementById("completedTasksList");
  try {
    const res = await fetch("/api/admin/tasks/completed");
    if (!res.ok) throw new Error(`HTTP Error ${res.status}`);

    completedTasksCache = await res.json();

    if (!completedTasksCache || completedTasksCache.length === 0) {
      container.innerHTML =
        '<p style="color: #64748b; font-size: 0.85rem;">No submitted audits yet.</p>';
      return;
    }

    container.innerHTML = "";
    completedTasksCache.forEach((t) => {
      const item = document.createElement("div");
      item.className = "completed-item";
      item.onclick = () => openReportModal(t.id);
      item.innerHTML = `
        <strong>${t.locationName || "Self-Initiated On-Site Audit"}</strong><br>
        Inspector: ${t.inspector ? t.inspector.username : "N/A"}<br>
        <small style="color: #64748b;">Risk Score: ${t.riskScore ? t.riskScore.toFixed(2) : "N/A"}</small><br>
        <span class="badge-completed">✓ View Submitted Report</span>
      `;
      container.appendChild(item);
    });

    loadStoreMarkers();
  } catch (err) {
    console.error("Failed to load completed tasks:", err);
    container.innerHTML =
      '<p style="color: #ef4444; font-size: 0.85rem;">Error loading completed audits.</p>';
  }
}

function openReportModal(taskId) {
  const task = completedTasksCache.find((t) => t.id === taskId);
  if (!task) return;

  document.getElementById("reportModalTitle").textContent =
    `Audit Report: ${task.locationName}`;
  document.getElementById("reportModalMeta").textContent =
    `Task ID: #${task.id} | Inspector: ${task.inspector ? task.inspector.username : "N/A"}`;

  const bodyEl = document.getElementById("reportModalBody");

  if (!task.reportData) {
    bodyEl.innerHTML =
      '<p style="color: #64748b;">No report payload attached to this task.</p>';
    document.getElementById("reportModal").classList.add("active");
    return;
  }

  try {
    let parsed = JSON.parse(task.reportData);
    if (typeof parsed === "string") parsed = JSON.parse(parsed);
    if (!Array.isArray(parsed)) parsed = [parsed];

    activeReportList = parsed;
    renderReportTabsAndContent(0);
  } catch (e) {
    console.warn("Displaying raw text fallback:", e);
    bodyEl.textContent = task.reportData;
  }

  document.getElementById("reportModal").classList.add("active");
}

function renderReportTabsAndContent(selectedIndex) {
  const bodyEl = document.getElementById("reportModalBody");
  if (!activeReportList || activeReportList.length === 0) return;

  let tabsHtml = `<div class="report-tabs">`;
  activeReportList.forEach((rpt, idx) => {
    const isCompliant = rpt.is_compliant;
    const statusIcon = isCompliant ? "✓" : "✕";
    tabsHtml += `
      <button class="report-tab-btn ${idx === selectedIndex ? "active" : ""}" onclick="renderReportTabsAndContent(${idx})">
        Product #${idx + 1} (${statusIcon})
      </button>
    `;
  });
  tabsHtml += `</div>`;

  const report = activeReportList[selectedIndex];


  let imagesArray = [];
  if (Array.isArray(report.productImages) && report.productImages.length > 0) {
    imagesArray = report.productImages;
  } else if (report.productImage) {
    imagesArray = [report.productImage];
  }


  let imageHtml = "";
  if (imagesArray.length > 0) {
    const imgElements = imagesArray.map((imgSrc, idx) => `
      <div style="display: inline-block; margin: 4px;">
        <img src="${imgSrc}" alt="Captured Product Package #${idx + 1}" 
             onclick="openLightboxModal('${imgSrc}')"
             style="max-height: 200px; width: auto; border-radius: 6px; border: 1px solid #475569; object-fit: contain; cursor: zoom-in;" 
             title="Click to Zoom Photo #${idx + 1}" />
        <p style="color: #94a3b8; font-size: 0.7rem; margin-top: 2px;">Photo #${idx + 1}</p>
      </div>
    `).join("");

    imageHtml = `
      <div style="margin-bottom: 14px; text-align: center; background: #0f172a; padding: 10px; border-radius: 8px; overflow-x: auto; white-space: nowrap;">
        ${imgElements}
        <p style="color: #94a3b8; font-size: 0.75rem; margin-top: 4px;">🔍 Click any photo above to enlarge & zoom in</p>
      </div>
    `;
  }

  const fields = [
    { key: "mrp", label: "MRP", rule: "Rule 6(1)(e)" },
    { key: "net_quantity", label: "Net Quantity", rule: "Rule 6(1)(c) & 13" },
    { key: "month_year", label: "Mfg Date", rule: "Rule 6(1)(d)" },
    { key: "generic_name", label: "Generic Name", rule: "Rule 6(1)(b)" },
    { key: "manufacturer_details", label: "Manufacturer", rule: "Rule 6(1)(a) & 10" },
    { key: "consumer_care", label: "Consumer Care", rule: "Rule 6(2)" },
  ];

  let tableRowsHtml = "";
  fields.forEach((field) => {
    const item = report[field.key];
    const val = item ? item.value : null;
    const isPresent = val !== null && val !== undefined && val !== "";
    tableRowsHtml += `
      <tr>
        <td><strong>${field.label}</strong><br><small style="color: #64748b;">${field.rule}</small></td>
        <td>${isPresent ? val : "Not Detected"}</td>
        <td style="color: ${isPresent ? "#16a34a" : "#dc2626"}; font-weight: bold;">
          ${isPresent ? "✓ Valid" : "✕ Violation"}
        </td>
      </tr>
    `;
  });

  let violationsHtml = "";
  if (report.violations && report.violations.length > 0) {
    violationsHtml = `
      <div style="margin-top: 14px; background: #fef2f2; border: 1px solid #fecaca; padding: 12px; border-radius: 6px;">
        <strong style="color: #991b1b;">Compliance Action Items / Violations:</strong>
        <ul style="margin-left: 20px; color: #991b1b; margin-top: 6px;">
          ${report.violations.map((v) => `<li>${v}</li>`).join("")}
        </ul>
      </div>
    `;
  }

  bodyEl.innerHTML = `
    ${tabsHtml}
    ${imageHtml}
    <div style="margin-bottom: 12px;">
      <strong>Inspection Result:</strong> 
      <span style="font-weight: bold; color: ${report.is_compliant ? "#16a34a" : "#dc2626"};">
        ${report.is_compliant ? "COMPLIANT" : "NON-COMPLIANT"}
      </span>
    </div>
    <table class="report-table-mini">
      <thead>
        <tr>
          <th>Attribute</th>
          <th>Extracted Value</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        ${tableRowsHtml}
      </tbody>
    </table>
    ${violationsHtml}
  `;
}


let currentZoomScale = 1.0;
let pointX = 0;
let pointY = 0;
let startX = 0;
let startY = 0;
let isDragging = false;

function updateImageTransform() {
  const imgEl = document.getElementById("lightboxImage");
  if (imgEl) {
    imgEl.style.transform = `translate(${pointX}px, ${pointY}px) scale(${currentZoomScale})`;
  }
}

function openLightboxModal(imgSrc) {
  const imgEl = document.getElementById("lightboxImage");
  imgEl.src = imgSrc;
  
  currentZoomScale = 1.0;
  pointX = 0;
  pointY = 0;
  updateImageTransform();

  const container = document.getElementById("lightboxContainer");
  if (container) {
    container.style.cursor = "grab";
  }

  document.getElementById("imageLightboxModal").classList.add("active");
  setupLightboxDragListeners();
}

function closeLightboxModal() {
  document.getElementById("imageLightboxModal").classList.remove("active");
}

function zoomImage(factor) {
  currentZoomScale *= factor;
  
  // Constrain scale between 0.5x and 5x
  if (currentZoomScale < 0.5) currentZoomScale = 0.5;
  if (currentZoomScale > 5.0) currentZoomScale = 5.0;

  updateImageTransform();
}

function resetImageZoom() {
  currentZoomScale = 1.0;
  pointX = 0;
  pointY = 0;
  updateImageTransform();
}

function setupLightboxDragListeners() {
  const container = document.getElementById("lightboxContainer");
  if (!container || container.dataset.dragInitialized) return;

  container.dataset.dragInitialized = "true";

  container.addEventListener("mousedown", (e) => {
    e.preventDefault();
    isDragging = true;
    startX = e.clientX - pointX;
    startY = e.clientY - pointY;
    container.style.cursor = "grabbing";
  });

  window.addEventListener("mousemove", (e) => {
    if (!isDragging) return;
    e.preventDefault();
    pointX = e.clientX - startX;
    pointY = e.clientY - startY;
    updateImageTransform();
  });

  window.addEventListener("mouseup", () => {
    if (isDragging) {
      isDragging = false;
      container.style.cursor = "grab";
    }
  });

  container.addEventListener("wheel", (e) => {
    e.preventDefault();
    const delta = e.deltaY < 0 ? 1.15 : 0.85;
    zoomImage(delta);
  }, { passive: false });
}

function closeReportModal() {
  document.getElementById("reportModal").classList.remove("active");
}

function openRegisterModal() {
  document.getElementById("regUsername").value = "";
  document.getElementById("registerModal").classList.add("active");
}

function closeRegisterModal() {
  document.getElementById("registerModal").classList.remove("active");
}

async function submitInspectorRegistration() {
  const username = document.getElementById("regUsername").value.trim();
  const lat = parseFloat(document.getElementById("regLat").value);
  const lng = parseFloat(document.getElementById("regLng").value);

  if (!username) {
    alert("Please enter an inspector username.");
    return;
  }

  const payload = {
    username: username,
    latitude: isNaN(lat) ? 19.076 : lat,
    longitude: isNaN(lng) ? 72.8777 : lng,
  };

  try {
    const response = await fetch("/api/admin/register-inspector", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      alert(`Inspector '${username}' successfully registered!`);
      closeRegisterModal();
      loadInspectors();
    } else {
      const err = await response.text();
      alert("Failed to register inspector: " + err);
    }
  } catch (err) {
    console.error("Registration error:", err);
    alert("Error registering inspector.");
  }
}

function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
  if (!lat1 || !lon1 || !lat2 || !lon2) return null;

  const R = 6371;
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function populateInspectorsWithDistance(targetLat, targetLng) {
  const select = document.getElementById("inspectorSelect");
  if (!select) return;

  select.innerHTML = '<option value="">-- Choose Inspector --</option>';
  if (!inspectorsList || inspectorsList.length === 0) return;

  const inspectorsWithDist = inspectorsList.map((inspector) => {
    let distKm = null;
    if (
      inspector.latitude &&
      inspector.longitude &&
      targetLat &&
      targetLng
    ) {
      distKm = calculateHaversineDistance(
        targetLat,
        targetLng,
        inspector.latitude,
        inspector.longitude,
      );
    }
    return { ...inspector, distanceKm: distKm };
  });

  inspectorsWithDist.sort((a, b) => {
    if (a.distanceKm === null) return 1;
    if (b.distanceKm === null) return -1;
    return a.distanceKm - b.distanceKm;
  });

  inspectorsWithDist.forEach((inspector) => {
    const option = document.createElement("option");
    option.value = inspector.id;

    const statusText = inspector.status || "AVAILABLE";
    const distLabel =
      inspector.distanceKm !== null
        ? ` — ${inspector.distanceKm.toFixed(1)} km away`
        : "";

    option.textContent = `${inspector.username} (${statusText})${distLabel}`;
    select.appendChild(option);
  });
}

function closeModal() {
  document.getElementById("taskModal").classList.remove("active");
}

async function submitTaskAssignment() {
  const inspectorId = document.getElementById("inspectorSelect").value;
  const locationName = document.getElementById("locationName").value;
  const riskScore = parseFloat(
    document.getElementById("riskScore").value,
  );

  if (!inspectorId || !locationName) {
    alert("Please fill out all task details.");
    return;
  }

  const payload = {
    inspectorId: parseInt(inspectorId),
    locationName: locationName,
    latitude: selectedCoords.lat,
    longitude: selectedCoords.lng,
    riskScore: riskScore,
  };

  try {
    const response = await fetch("/api/admin/assign-task", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      alert("Inspection task successfully requested!");
      closeModal();
      loadInspectors();
      loadCompletedTasks();
    } else {
      alert("Failed to assign task.");
    }
  } catch (err) {
    console.error("Assignment error:", err);
  }
}

loadHeatmapData();
loadStoreMarkers();
loadInspectors();
loadCompletedTasks();