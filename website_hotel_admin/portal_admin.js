// --- 1. FIREBASE CONFIG & INIT ---
const firebaseConfig = {
  apiKey: "AIzaSyBw6jDr8wRKeMMR7TiX8YiB0kO1wIfEmbE",
  authDomain: "roomserviceapk.firebaseapp.com",
  databaseURL: "https://roomserviceapk-default-rtdb.firebaseio.com",
  projectId: "roomserviceapk",
  storageBucket: "roomserviceapk.firebasestorage.app",
  messagingSenderId: "987842436715",
  appId: "1:987842436715:web:04d22839d4ca52c61e1b2e"
};

firebase.initializeApp(firebaseConfig);
const db = firebase.database();

let hotelId = "GangaHomes_001";
let allBookings = [];
let allRooms = [];
let pmsStartDate = new Date();
let revenueChartInstance = null;

// --- 2. INITIALIZATION ---
window.onload = () => {
    pmsStartDate.setHours(0,0,0,0);
    startRealtimeSync();
    updateLiveDate();
    switchTab('pms-grid');

    const picker = document.getElementById('pms-date-picker');
    if(picker) {
        picker.valueAsDate = pmsStartDate;
        picker.onchange = (e) => {
            pmsStartDate = new Date(e.target.value);
            renderPMSGrid();
        };
    }
};

// --- 3. CORE NAVIGATION ---
window.switchTab = (tabId) => {
    const implemented = ['pms-grid', 'dashboard', 'bookings', 'guests', 'rooms', 'reports', 'settings'];
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.add('hidden'));
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));

    const targetId = implemented.includes(tabId) ? `tab-${tabId}` : 'tab-coming-soon';
    const target = document.getElementById(targetId);
    if(target) target.classList.remove('hidden');

    document.querySelector(`.nav-item[data-tab="${tabId}"]`)?.classList.add('active');

    if(tabId === 'pms-grid') renderPMSGrid();
    if(tabId === 'dashboard') initDashboardCharts();
    if(tabId === 'reports') initReportCharts();
};

// --- 4. DATA SYNCHRONIZATION ---
function startRealtimeSync() {
    listenForRooms();
    listenForBookings();
    listenForSettings();
}

function listenForRooms() {
    db.ref(`hotels/${hotelId}/rooms`).on('value', snap => {
        allRooms = [];
        snap.forEach(c => allRooms.push(c.val()));
        renderRoomGrid();
        renderPMSGrid();
        updateRoomSelect();
    });
}

function listenForBookings() {
    db.ref(`hotels/${hotelId}/bookings`).on('value', snap => {
        allBookings = [];
        snap.forEach(c => allBookings.push(c.val()));
        allBookings.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

        renderPMSGrid();
        renderDashboardBookings();
        renderFullBookings();
        renderGuestTable();
        updateStats();
    });
}

function listenForSettings() {
    db.ref(`hotels/${hotelId}/business_details`).on('value', snap => {
        const d = snap.val();
        if(!d) return;
        document.getElementById('nav-hotel-name').innerText = (d.hotelName || 'GANGA HOMES').toUpperCase();
        document.getElementById('set-hotel-name').value = d.hotelName || '';
        document.getElementById('set-hotel-address').value = d.address || '';
        document.getElementById('set-hotel-phone').value = d.phone || '';
    });
}

// --- 5. PMS GRID (TIMELINE) ---
function renderPMSGrid() {
    const container = document.getElementById('pms-timeline-grid');
    if(!container) return;

    const daysToShow = 21;
    const cellWidth = 100;

    let html = `
        <div class="pms-room-column">
            <div class="pms-room-header">Room / Units</div>
            ${allRooms.map(r => `
                <div class="pms-room-row">
                    <h4>${r.roomNumber}</h4>
                    <span>${r.roomType}</span>
                </div>
            `).join('')}
        </div>
        <div class="pms-timeline-data">
            <div class="pms-days-row">
                ${Array.from({length: daysToShow}).map((_, i) => {
                    const date = new Date(pmsStartDate);
                    date.setDate(date.getDate() + i);
                    const isToday = isSameDay(date, new Date());
                    return `
                        <div class="pms-day-cell ${isToday?'today':''}">
                            <b>${date.getDate()}</b>
                            <span>${date.toLocaleDateString('en-GB', {weekday: 'short'})}</span>
                        </div>
                    `;
                }).join('')}
            </div>
            ${allRooms.map(r => `
                <div class="pms-grid-row">
                    ${Array.from({length: daysToShow}).map(() => `<div class="pms-grid-cell"></div>`).join('')}
                    ${renderRoomBookingBlocks(r.roomNumber, daysToShow, cellWidth)}
                </div>
            `).join('')}
        </div>
    `;
    container.innerHTML = html;
}

function renderRoomBookingBlocks(roomNo, daysCount, width) {
    const startTs = pmsStartDate.getTime();
    const endTs = startTs + (daysCount * 86400000);

    return allBookings.filter(b => b.roomNumber === roomNo && b.status !== 'CANCELLED')
        .map(b => {
            if(b.checkOutDate < startTs || b.checkInDate > endTs) return '';

            const start = Math.max(b.checkInDate, startTs);
            const end = Math.min(b.checkOutDate, endTs);

            const left = ((start - startTs) / 86400000) * width;
            const bWidth = ((end - start) / 86400000) * width;

            return `
                <div class="booking-block ${b.status.toLowerCase()}" style="left:${left}px; width:${bWidth}px;">
                    ${b.guestName}
                </div>
            `;
        }).join('');
}

// --- 6. UI RENDERING ---
function renderDashboardBookings() {
    const body = document.getElementById('dash-bookings-body');
    if(!body) return;
    body.innerHTML = allBookings.slice(0, 5).map(b => `
        <tr>
            <td style="color:var(--primary); font-family:monospace; font-weight:700;">#${(b.id || '').slice(-6)}</td>
            <td><b>${b.guestName}</b></td>
            <td>${b.roomNumber || 'TBA'}</td>
            <td><span class="status-pill ${(b.status || 'BOOKED').toLowerCase()}">${b.status || 'BOOKED'}</span></td>
        </tr>
    `).join('');
}

function renderFullBookings() {
    const body = document.getElementById('bookings-table-body');
    if(!body) return;
    body.innerHTML = allBookings.map(b => `
        <tr>
            <td>#BK-${(b.id || '').slice(-4).toUpperCase()}</td>
            <td><b>${b.guestName}</b></td>
            <td>${b.guestPhone || '-'}</td>
            <td>${b.roomNumber || 'TBA'}</td>
            <td>${fmtDate(b.checkInDate)} - ${fmtDate(b.checkOutDate)}</td>
            <td style="font-weight:800;">₹${(b.totalAmount || 0).toLocaleString()}</td>
            <td><span class="status-pill ${(b.status || 'BOOKED').toLowerCase()}">${b.status || 'BOOKED'}</span></td>
            <td><button class="btn-sm btn-outline" onclick="deleteBooking('${b.id}')">✕</button></td>
        </tr>
    `).join('');
}

function renderGuestTable() {
    const body = document.getElementById('guests-table-body');
    if(!body) return;
    const guests = {};
    allBookings.forEach(b => {
        if(!guests[b.guestName]) guests[b.guestName] = { phone: b.guestPhone, email: '-', stays: 0, last: 0 };
        guests[b.guestName].stays++;
        if(b.checkInDate > guests[b.guestName].last) guests[b.guestName].last = b.checkInDate;
    });
    body.innerHTML = Object.keys(guests).map(name => `
        <tr>
            <td><b>${name}</b></td>
            <td>${guests[name].phone || '-'}</td>
            <td>${guests[name].email}</td>
            <td>${guests[name].stays}</td>
            <td>${fmtDate(guests[name].last)}</td>
        </tr>
    `).join('');
}

function renderRoomGrid() {
    const grid = document.getElementById('rooms-grid');
    if(!grid) return;
    grid.innerHTML = allRooms.map(r => `
        <div class="panel" style="padding:0; overflow:hidden;">
            <div style="height:120px; background:#f8fafc; display:flex; align-items:center; justify-content:center; font-size:3rem; position:relative;">
                🛌 <div style="position:absolute; top:12px; right:12px;"><span class="status-pill ${r.isAvailable?'confirmed':'pending'}">${r.isAvailable?'Vacant':'Occupied'}</span></div>
            </div>
            <div style="padding:16px;">
                <h4 style="font-weight:800;">Room ${r.roomNumber}</h4>
                <p style="font-size:0.75rem; color:var(--text-muted); font-weight:700;">${r.roomType}</p>
            </div>
        </div>
    `).join('');
}

function updateStats() {
    const today = new Date().setHours(0,0,0,0);
    const ins = allBookings.filter(b => isSameDay(b.checkInDate, today)).length;
    const outs = allBookings.filter(b => isSameDay(b.checkOutDate, today)).length;
    const occ = allRooms.filter(r => !r.isAvailable).length;

    if(document.getElementById('stat-checkins')) document.getElementById('stat-checkins').innerText = ins.toString().padStart(2, '0');
    if(document.getElementById('stat-checkouts')) document.getElementById('stat-checkouts').innerText = outs.toString().padStart(2, '0');
    if(document.getElementById('stat-occupied')) document.getElementById('stat-occupied').innerText = occ.toString().padStart(2, '0');
    if(document.getElementById('stat-vacant')) document.getElementById('stat-vacant').innerText = (allRooms.length - occ).toString().padStart(2, '0');

    const revTotal = allBookings.reduce((s, b) => s + (parseFloat(b.totalAmount) || 0), 0);
    if(document.getElementById('dash-rev-total')) document.getElementById('dash-rev-total').innerText = "₹ " + revTotal.toLocaleString();
}

// --- 7. CHARTS ---
function initDashboardCharts() {
    const ctx = document.getElementById('revenueChart');
    if(!ctx) return;
    if(revenueChartInstance) revenueChartInstance.destroy();
    revenueChartInstance = new Chart(ctx, {
        type: 'line',
        data: { labels: ['12 AM', '6 AM', '12 PM', '6 PM', '12 AM'], datasets: [{ data: [2000, 8000, 25000, 15000, 32000], borderColor: '#1976D2', backgroundColor: 'rgba(25,118,210,0.1)', fill: true, tension: 0.4, pointRadius: 0 }] },
        options: { plugins: { legend: { display: false } }, maintainAspectRatio: false, scales: { x: { display: false }, y: { display: false } } }
    });
}

function initReportCharts() {
    const ctx = document.getElementById('revenueReportChart');
    if(!ctx) return;
    new Chart(ctx, {
        type: 'line',
        data: { labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'], datasets: [{ label: 'Revenue', data: [12000, 25000, 18000, 45000, 32000, 50000, 42000], borderColor: '#1976D2', tension: 0.4, fill: true, backgroundColor: 'rgba(25,118,210,0.05)' }] },
        options: { maintainAspectRatio: false }
    });
}

// --- 8. HELPERS ---
function updateLiveDate() {
    const el = document.getElementById('dash-date');
    if(el) el.innerText = new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric', weekday: 'long' });
}
function fmtDate(ts) { return ts ? new Date(ts).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) : '-'; }
function isSameDay(d1, d2) {
    const dt1 = new Date(d1);
    const dt2 = new Date(d2);
    return dt1.getFullYear() === dt2.getFullYear() && dt1.getMonth() === dt2.getMonth() && dt1.getDate() === dt2.getDate();
}

window.deleteBooking = (id) => { if(confirm("Delete reservation?")) db.ref(`hotels/${hotelId}/bookings/${id}`).remove(); };
window.saveAllSettings = () => {
    const data = { hotelName: document.getElementById('set-hotel-name').value, address: document.getElementById('set-hotel-address').value, phone: document.getElementById('set-hotel-phone').value };
    db.ref(`hotels/${hotelId}/business_details`).update(data).then(() => alert("Settings Updated!"));
};

function updateRoomSelect() {
    const select = document.getElementById('nb-room-select');
    if(select) select.innerHTML = allRooms.map(r => `<option value="${r.roomNumber}">${r.roomNumber} - ${r.roomType}</option>`).join('');
}

window.openBookingModal = () => document.getElementById('booking-modal').classList.remove('hidden');
window.closeBookingModal = () => document.getElementById('booking-modal').classList.add('hidden');

window.confirmNewBooking = () => {
    const name = document.getElementById('nb-name').value;
    const phone = document.getElementById('nb-phone').value;
    const cin = document.getElementById('nb-in').value;
    const cout = document.getElementById('nb-out').value;
    const room = document.getElementById('nb-room-select').value;

    if(!name || !cin || !cout) return alert("Fill required fields");

    const id = 'BK_PMS_' + Date.now();
    db.ref(`hotels/${hotelId}/bookings/${id}`).set({
        id, hotelId, guestName: name, guestPhone: phone,
        checkInDate: new Date(cin).getTime(), checkOutDate: new Date(cout).getTime(),
        roomNumber: room, status: 'BOOKED', totalAmount: 2500, advancePaid: 0, timestamp: Date.now()
    }).then(() => { alert("Reservation Saved!"); closeBookingModal(); });
};
