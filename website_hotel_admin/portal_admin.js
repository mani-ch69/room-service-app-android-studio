// --- 1. REUSABLE UI COMPONENTS ---
const UI = {
    card: (title, content, action = '') => `
        <div class="card-ui">
            <div class="card-head">
                <span>${title}</span>
                ${action ? `<div class="card-action">${action}</div>` : ''}
            </div>
            <div class="card-ui-body">${content}</div>
        </div>
    `,
    badge: (text, type = 'ok') => `<span class="status-tag ${type.toLowerCase()}">${text}</span>`,
    pill: (text, type = 'open') => `<span class="status-pill ${type.toLowerCase()}">${text}</span>`,
    infoBanner: (text, icon = 'ℹ️') => `
        <div style="background:#FFF7E6; border:1px solid #FFD591; padding:16px; border-radius:4px; display:flex; gap:12px; align-items:flex-start; margin-bottom:24px;">
            <span style="font-size:1.2rem;">${icon}</span>
            <div style="font-size:0.85rem; color:#874D00;">${text}</div>
        </div>
    `,
    table: (headers, rows) => `
        <table class="table-reservations">
            <thead><tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr></thead>
            <tbody>${rows.length ? rows.join('') : '<tr><td colspan="' + headers.length + '" style="text-align:center;">No data available</td></tr>'}</tbody>
        </table>
    `
};

// --- 2. FIREBASE CONFIG ---
const firebaseConfig = {
  apiKey: "AIzaSyBw6jDr8wRKeMMR7TiX8YiB0kO1wIfEmbE",
  authDomain: "roomserviceapk.firebaseapp.com",
  databaseURL: "https://roomserviceapk-default-rtdb.firebaseio.com",
  projectId: "roomserviceapk",
  storageBucket: "roomserviceapk.firebasestorage.app",
  messagingSenderId: "987842436715",
  appId: "1:987842436715:web:def97213089cf3121e1b2e",
  measurementId: "G-SW9WNGGDYF"
};

firebase.initializeApp(firebaseConfig);
const db = firebase.database();

let hotelId = "GangaHomes_001";
let allRooms = [];

// --- 3. CORE NAVIGATION ---
window.toggleDropdown = (e, tabId) => {
    if (e) e.stopPropagation();
    const item = e ? e.currentTarget : null;
    if (!item) return;
    const isShowing = item.classList.contains('show-dropdown');
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('show-dropdown'));
    if (!isShowing) item.classList.add('show-dropdown');
};

document.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('show-dropdown'));
});

window.switchTab = (tabId) => {
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.add('hidden'));
    const target = document.getElementById(`tab-${tabId}`);
    if(target) target.classList.remove('hidden');
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    if(window.event && window.event.currentTarget && window.event.currentTarget.classList.contains('nav-item')) {
        window.event.currentTarget.classList.add('active');
    }
    if(tabId === 'reservations') renderReservations();
    if(tabId === 'pms-grid') renderPMSGrid();
};

window.toggleMoreFilters = () => {
    const panel = document.getElementById('filters-expanded-panel');
    if(panel) panel.classList.toggle('hidden');
};

window.switchSubTab = (subId) => {
    document.querySelectorAll('.sub-pane').forEach(p => p.classList.add('hidden'));
    const target = document.getElementById(`sub-${subId}`);
    if(target) target.classList.remove('hidden');
    document.querySelectorAll('.sub-tab').forEach(t => t.classList.remove('active'));
    if(window.event && window.event.currentTarget) window.event.currentTarget.classList.add('active');
};

// --- 4. DATE PICKER COMPONENT (REUSABLE) ---
function setupSyncedPickers(fromSelector, untilSelector, onNightsUpdate = null) {
    let fromPicker;
    const untilPicker = flatpickr(untilSelector, {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "Y-m-d",
        defaultDate: new Date().getTime() + 24 * 60 * 60 * 1000,
        onChange: function(selectedDates) {
            if (selectedDates.length === 1 && fromPicker) {
                const currentRange = fromPicker.selectedDates;
                if (currentRange.length > 0) {
                    fromPicker.setDate([currentRange[0], selectedDates[0]], false);
                    if(onNightsUpdate) {
                        const diff = selectedDates[0] - currentRange[0];
                        onNightsUpdate(Math.ceil(diff / (1000 * 60 * 60 * 24)));
                    }
                }
            }
        }
    });

    fromPicker = flatpickr(fromSelector, {
        mode: "range",
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "Y-m-d",
        defaultDate: [new Date(), new Date().getTime() + 24 * 60 * 60 * 1000],
        onReady: function(selectedDates, dateStr, instance) {
            if (selectedDates.length > 0) {
                instance.altInput.value = instance.formatDate(selectedDates[0], "Y-m-d");
            }
        },
        onValueUpdate: function(selectedDates, dateStr, instance) {
            if (selectedDates.length > 0) {
                instance.altInput.value = instance.formatDate(selectedDates[0], "Y-m-d");
            }
        },
        onChange: function(selectedDates) {
            if (selectedDates.length === 2) {
                untilPicker.setDate(selectedDates[1], false);
                if(onNightsUpdate) {
                    const diff = selectedDates[1] - selectedDates[0];
                    onNightsUpdate(Math.ceil(diff / (1000 * 60 * 60 * 24)));
                }
            }
        }
    });
}

// --- 5. INITIALIZATION & DATA SYNC ---
window.onload = () => {
    startRealtimeSync();
    renderAmenities();
    initDashboard();

    // Reusable Calendar Component implementation for both sections
    setupSyncedPickers("#res-from-date", "#res-to-date");
    setupSyncedPickers("#mb-check-in", "#mb-check-out", (nights) => {
        const nightEl = document.getElementById('mb-nights');
        if(nightEl) {
            nightEl.value = nights;
            calculateBookingTotal();
        }
    });

    // Calendar Range Picker
    flatpickr("#pms-date-range", {
        mode: "range",
        dateFormat: "M j, Y",
        defaultDate: [new Date(), new Date().getTime() + 30 * 24 * 60 * 60 * 1000],
        onChange: () => renderPMSGrid()
    });
};

function startRealtimeSync() {
    db.ref(`hotels/${hotelId}/business_details`).on('value', snap => {
        const d = snap.val();
        if(d) {
            if(document.getElementById('header-hotel-name')) document.getElementById('header-hotel-name').innerText = d.hotelName;
            if(document.getElementById('disp-hotel-name')) document.getElementById('disp-hotel-name').innerText = d.hotelName;
            if(document.getElementById('disp-hotel-addr')) document.getElementById('disp-hotel-addr').innerText = d.address;
        }
    });

    db.ref(`hotels/${hotelId}/rooms`).on('value', snap => {
        allRooms = [];
        snap.forEach(c => allRooms.push(c.val()));
        renderRoomPhotoSections();
        renderRoomDetails();
    });
}

// --- 6. DASHBOARD & BOOKING LOGIC ---
function initDashboard() {
    renderDashBookings();
}

function renderDashBookings() {
    const container = document.getElementById('dash-recent-bookings');
    if(!container) return;
    db.ref(`hotels/${hotelId}/bookings`).limitToLast(5).on('value', snap => {
        const rows = [];
        snap.forEach(child => {
            const b = child.val();
            const displayId = b.bookingNumber || b.id.slice(-8);
            rows.push(`
                <tr>
                    <td>${displayId}</td>
                    <td><b>${b.guestName}</b></td>
                    <td>Room ${b.roomNumber || '-'}</td>
                    <td>${new Date(b.checkInDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short' })}</td>
                    <td>${new Date(b.checkOutDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short' })}</td>
                    <td>${UI.badge(b.status || 'Confirmed', b.status || 'ok')}</td>
                </tr>
            `);
        });
        container.innerHTML = rows.reverse().join('');
    });
}

window.openManualBooking = () => {
    document.getElementById('manual-booking-modal').classList.remove('hidden');
    const typeSelect = document.getElementById('mb-room-type');
    const types = [...new Set(allRooms.map(r => r.roomType))];
    typeSelect.innerHTML = '<option value="">Select type</option>' + types.map(t => `<option value="${t}">${t}</option>`).join('');
};

window.closeManualBooking = () => document.getElementById('manual-booking-modal').classList.add('hidden');

window.filterVacantRooms = () => {
    const type = document.getElementById('mb-room-type').value;
    const roomSelect = document.getElementById('mb-room-id');
    const vacant = allRooms.filter(r => r.roomType === type);
    roomSelect.innerHTML = vacant.map(r => `<option value="${r.roomNumber || r.id}">${r.roomNumber || 'Room ' + r.id.slice(-3)}</option>`).join('');
};

window.calculateBookingTotal = () => {
    const nights = parseInt(document.getElementById('mb-nights').value) || 1;
    const rent = parseFloat(document.getElementById('mb-rent').value) || 0;
    const discountPercent = parseFloat(document.getElementById('mb-discount').value) || 0;
    const advance = parseFloat(document.getElementById('mb-advance').value) || 0;
    const fullPay = document.getElementById('mb-full-pay').checked;

    const subtotal = nights * rent;
    const discountAmount = (subtotal * discountPercent) / 100;
    const total = subtotal - discountAmount;
    document.getElementById('mb-total').value = total.toLocaleString();

    if(fullPay) {
        document.getElementById('mb-advance').value = total;
        document.getElementById('mb-remaining').value = "0";
        document.getElementById('mb-advance').disabled = true;
    } else {
        document.getElementById('mb-advance').disabled = false;
        const remaining = total - advance;
        document.getElementById('mb-remaining').value = remaining.toLocaleString();
    }
};

window.toggleFullPay = () => calculateBookingTotal();

window.submitManualBooking = () => {
    const guestName = document.getElementById('mb-guest-name').value;
    const roomType = document.getElementById('mb-room-type').value;
    if(!guestName || !roomType) { alert("Bhai, Guest Name aur Room Type toh bhar do!"); return; }

    const checkInEl = document.getElementById('mb-check-in');
    const dates = checkInEl && checkInEl._flatpickr ? checkInEl._flatpickr.selectedDates : [];
    if (dates.length < 2) { alert("Bhai, Stay Dates toh sahi se select kar lo!"); return; }

    const bookingNumber = Math.floor(1000000000 + Math.random() * 9000000000).toString();

    const newBooking = {
        id: "MB-" + Date.now(),
        bookingNumber: bookingNumber,
        guestName: guestName,
        roomNumber: roomType, // Using room type as the identifier
        checkInDate: dates[0].toISOString(),
        checkOutDate: dates[1].toISOString(),
        totalAmount: parseFloat(document.getElementById('mb-total').value.replace(/,/g, '')),
        status: "Confirmed",
        timestamp: Date.now()
    };

    db.ref(`hotels/${hotelId}/bookings`).push(newBooking).then(() => {
        closeManualBooking();
        alert("Manual Booking Successful! ID: " + bookingNumber);
    });
};

// --- 7. PROPERTY MANAGEMENT ---
function renderReservations() {
    const container = document.getElementById('reservations-table-body');
    if(!container) return;
    db.ref(`hotels/${hotelId}/bookings`).on('value', snap => {
        const rows = [];
        snap.forEach(child => {
            const b = child.val();
            const cin = new Date(b.checkInDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
            const cout = new Date(b.checkOutDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
            const bookedOn = b.timestamp ? new Date(b.timestamp).toLocaleDateString('en-GB', { day:'2-digit', month:'short' }) : '-';
            const displayId = b.bookingNumber || b.id.slice(-8);
            rows.push(`
                <tr>
                    <td><span class="guest-name-link">${b.guestName}</span></td>
                    <td>${cin}</td>
                    <td>${cout}</td>
                    <td>1 × ${b.roomNumber || 'Deluxe'}</td>
                    <td>${bookedOn}</td>
                    <td>${UI.badge(b.status || 'OK', b.status || 'ok')}</td>
                    <td>₹ ${(b.totalAmount || 0).toLocaleString()}</td>
                    <td>₹ 0</td>
                    <td><span class="booking-id-link">${displayId}</span></td>
                </tr>
            `);
        });
        container.innerHTML = rows.join('') || '<tr><td colspan="9" style="text-align:center;">No reservations found</td></tr>';
    });
}

function renderAmenities() {
    const amenities = ["Air conditioning", "Balcony", "View", "Flat-screen TV", "Terrace", "Electric kettle", "Toilet paper", "Towels", "Linens"];
    const container = document.getElementById('amenities-list');
    if(!container) return;
    container.innerHTML = amenities.map(name => `
        <div class="amenity-item">
            <span class="amenity-name">${name}</span>
            <div class="segmented-control">
                <div class="seg-btn" onclick="toggleSeg(this)">All</div>
                <div class="seg-btn" onclick="toggleSeg(this)">Some</div>
                <div class="seg-btn active" onclick="toggleSeg(this)">None</div>
            </div>
        </div>
    `).join('');
}

window.toggleSeg = (el) => {
    const parent = el.parentElement;
    parent.querySelectorAll('.seg-btn').forEach(b => b.classList.remove('active'));
    el.classList.add('active');
};

window.openAddRoomModal = () => {
    document.getElementById('add-room-modal').classList.remove('hidden');
    renderAddRoomForm();
};

window.closeAddRoomModal = () => document.getElementById('add-room-modal').classList.add('hidden');

function renderAddRoomForm() {
    const container = document.getElementById('add-room-form-container');
    if(!container) return;
    container.innerHTML = `
        <div class="form-section-card">
            <h3>Please select</h3>
            <div class="form-group">
                <label>Room type</label>
                <select class="form-select" id="field-room-type">
                    <option>Please select</option>
                    <option>Deluxe Double Room</option>
                    <option>Single Room</option>
                    <option>Twin Room</option>
                    <option>Suite</option>
                </select>
            </div>
            <div class="form-row-flex">
                <div class="form-group">
                    <label>Number of rooms</label>
                    <input type="number" class="form-input-text" value="1" id="field-room-count">
                </div>
                <div class="form-group">
                    <label>Smoking policy</label>
                    <select class="form-select" id="field-smoking">
                        <option>Non-smoking</option>
                        <option>Smoking</option>
                    </select>
                </div>
            </div>
        </div>
        <div class="form-section-card">
            <h3>Room location</h3>
            <div class="form-group">
                <label>Floor Level</label>
                <select class="form-select">
                    <option>Ground floor</option>
                    <option>1st floor</option>
                </select>
            </div>
        </div>
    `;
}

window.saveNewRoom = () => {
    const roomType = document.getElementById('field-room-type').value;
    if(roomType === 'Please select') { alert('Bhai, Room type toh select kar lo!'); return; }
    const newRoom = { roomType: roomType, maxGuests: 2, maxAdults: 2, maxChildren: 0, id: "room_" + Math.random().toString(36).substr(2, 9), photos: [] };
    db.ref(`hotels/${hotelId}/rooms`).push(newRoom).then(() => closeAddRoomModal());
};

function renderRoomDetails() {
    const container = document.getElementById('room-details-container');
    if(!container) return;
    container.innerHTML = allRooms.map(room => `
        <div class="room-card">
            <div class="room-card-img-wrap">
                <img src="${room.photos && room.photos[0] ? room.photos[0] : 'https://i.ibb.co/6P0f9pL/ganga-homes-logo.jpg'}">
                <div class="room-card-info-bar"><h4>${room.roomType}</h4><p>(${room.id.slice(-5)})</p></div>
            </div>
            <div class="room-card-body">
                <div class="room-stat-line">Max Guests: <b>${room.maxGuests}</b></div>
            </div>
        </div>
    `).join('') + `
        <div class="create-room-card" onclick="openAddRoomModal()">
            <h3>Create a new room</h3>
            <div class="plus-circle">+</div>
        </div>
    `;
}

function renderRoomPhotoSections() {
    const container = document.getElementById('room-photos-container');
    if(!container) return;
    const distinctTypes = [...new Set(allRooms.map(r => r.roomType))];
    container.innerHTML = distinctTypes.map(type => UI.card(`${type} Gallery`, '<div class="gallery-grid"><div class="photo-add-btn">+</div></div>', '<button class="btn btn-outline btn-sm">+ Add Photos</button>')).join('');
}

// --- 8. PMS GRID (CALENDAR) LOGIC ---

function renderPMSGrid() {
    const headerContainer = document.getElementById('pms-date-header');
    const bodyContainer = document.getElementById('pms-grid-content');
    if(!headerContainer || !bodyContainer) return;

    // 1. Generate 31 days (to match image density)
    const dates = [];
    const now = new Date();
    for(let i=0; i<31; i++) {
        const d = new Date(now);
        d.setDate(now.getDate() + i);
        dates.push(d);
    }

    // 2. Render Header
    headerContainer.innerHTML = dates.map(d => `
        <div class="pms-date-cell ${[0,6].includes(d.getDay()) ? 'weekend' : ''}">
            <label>${d.toLocaleDateString('en-GB', { weekday: 'short' })}</label>
            <span>${d.getDate().toString().padStart(2, '0')}</span>
        </div>
    `).join('');

    // 3. Render Body (Room Rows)
    bodyContainer.innerHTML = allRooms.map(room => {
        const roomName = room.roomType;
        const roomId = room.id.slice(-5);

        return `
            <div class="pms-room-block">
                <div class="pms-room-header">
                    <h4>${roomName} <small style="color:var(--text-muted); font-weight:400; font-size:0.75rem;">(Room ID: ${roomId})</small></h4>
                    <div style="display:flex; gap:12px; align-items:center;">
                        <a href="#">Sync calendar</a>
                        <button class="btn-bulk-blue" onclick="alert('Bulk edit')">Bulk edit</button>
                    </div>
                </div>

                <!-- Status Row -->
                <div class="pms-row row-status">
                    <div class="pms-row-label">Room status</div>
                    <div class="pms-cells-wrap">
                        ${dates.map((d, i) => `
                            <div class="pms-cell ${i < 8 ? 'status-closed' : 'status-open'}">
                                ${i < 8 ? 'Closed' : 'Bookable'}
                            </div>
                        `).join('')}
                    </div>
                </div>

                <!-- Inventory Row -->
                <div class="pms-row">
                    <div class="pms-row-label">
                        <span>Rooms to Sell</span>
                        <a href="#" style="font-size:0.65rem;">Bulk edit</a>
                    </div>
                    <div class="pms-cells-wrap">
                        ${dates.map(() => `<div class="pms-cell">1</div>`).join('')}
                    </div>
                </div>

                <!-- Net Booked Row -->
                <div class="pms-row">
                    <div class="pms-row-label">Net Booked</div>
                    <div class="pms-cells-wrap">
                        ${dates.map((d, i) => `
                            <div class="pms-cell" style="background:#F1F5F9;">
                                ${i === 12 ? '<span class="booked-pill">1</span>' : '0'}
                            </div>
                        `).join('')}
                    </div>
                </div>

                <!-- Rate Plans -->
                <div class="pms-row row-rate">
                    <div class="pms-row-label">
                        <span class="chevron">⌄</span>
                        <input type="checkbox" checked>
                        <span>Standard rate</span>
                        <a href="#">x 2 Edit</a>
                    </div>
                    <div class="pms-cells-wrap">
                        ${dates.map((d, i) => `
                            <div class="pms-cell ${i < 8 ? 'closed-overlay' : ''}">
                                <span class="price-symbol">₹</span>
                                <span class="price-val">1200</span>
                            </div>
                        `).join('')}
                    </div>
                </div>

                <div class="pms-row row-rate">
                    <div class="pms-row-label">
                        <span class="chevron">⌄</span>
                        <input type="checkbox">
                        <span>Non-refundable</span>
                        <a href="#">x 2 Edit</a>
                    </div>
                    <div class="pms-cells-wrap">
                        ${dates.map((d, i) => `
                            <div class="pms-cell ${i < 8 ? 'closed-overlay' : ''}">
                                <span class="price-symbol">₹</span>
                                <span class="price-val">1080</span>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // 4. Sync Horizontal Scrolling
    const dataRowWraps = document.querySelectorAll('.pms-cells-wrap');
    const headerWrap = document.querySelector('#pms-date-header');

    const handleScroll = (e) => {
        const scrollLeft = e.target.scrollLeft;
        headerWrap.scrollLeft = scrollLeft;
        dataRowWraps.forEach(wrap => {
            if (wrap !== e.target) wrap.scrollLeft = scrollLeft;
        });
    };

    dataRowWraps.forEach(wrap => wrap.addEventListener('scroll', handleScroll));
}
