// --- 1. REUSABLE UI COMPONENTS (THE "COMPONENTS" SYSTEM) ---
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

// --- 2. FIREBASE CONFIG & INIT ---
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
let allBookings = [];

// --- 3. CORE NAVIGATION ---
window.toggleDropdown = (e, tabId) => {
    if (e) e.stopPropagation();
    const item = e ? e.currentTarget : null;
    if (!item) return;

    const isShowing = item.classList.contains('show-dropdown');

    // Close all other dropdowns
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('show-dropdown'));

    // Toggle current
    if (!isShowing) item.classList.add('show-dropdown');
};

// Close dropdowns when clicking outside
document.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('show-dropdown'));
});

window.switchTab = (tabId) => {
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.add('hidden'));
    const target = document.getElementById(`tab-${tabId}`);
    if(target) target.classList.remove('hidden');

    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));

    // Highlight the active menu item
    // If called via click, the event will help us find the target
    if(window.event && window.event.currentTarget && window.event.currentTarget.classList.contains('nav-item')) {
        window.event.currentTarget.classList.add('active');
    } else {
        // Fallback: Find by text or data-tab if we implement it
    }

    if(tabId === 'reservations') renderReservations();
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

// --- 4. DATA SYNC ---
window.onload = () => {
    startRealtimeSync();
    renderAmenities();
    initPickers();
    initDashboard();
};

function initPickers() {
    let fromPicker;
    const untilPicker = flatpickr("#res-to-date", {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "Y-m-d",
        defaultDate: new Date().getTime() + 24 * 60 * 60 * 1000, // Next day
        onChange: function(selectedDates) {
            if (selectedDates.length === 1 && fromPicker) {
                const currentRange = fromPicker.selectedDates;
                if (currentRange.length > 0) {
                    fromPicker.setDate([currentRange[0], selectedDates[0]], false);
                }
            }
        }
    });

    fromPicker = flatpickr("#res-from-date", {
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
            // Override display to only show start date in the 'From' input
            if (selectedDates.length > 0) {
                const start = instance.formatDate(selectedDates[0], "Y-m-d");
                instance.altInput.value = start;
            }
        },
        onChange: function(selectedDates) {
            if (selectedDates.length === 2) {
                untilPicker.setDate(selectedDates[1], false);
            }
        }
    });

    // New Manual Booking Pickers (Synced Logic)
    let mbFromPicker;
    const mbUntilPicker = flatpickr("#mb-check-out", {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "Y-m-d",
        defaultDate: new Date().getTime() + 24 * 60 * 60 * 1000, // Next day
        onChange: function(selectedDates) {
            if (selectedDates.length === 1 && mbFromPicker) {
                const currentRange = mbFromPicker.selectedDates;
                if (currentRange.length > 0) {
                    mbFromPicker.setDate([currentRange[0], selectedDates[0]], false);
                    updateMbNights(currentRange[0], selectedDates[0]);
                }
            }
        }
    });

    mbFromPicker = flatpickr("#mb-check-in", {
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
                mbUntilPicker.setDate(selectedDates[1], false);
                updateMbNights(selectedDates[0], selectedDates[1]);
            }
        }
    });
}

function updateMbNights(start, end) {
    if (start && end) {
        const diff = end - start;
        const nights = Math.ceil(diff / (1000 * 60 * 60 * 24));
        document.getElementById('mb-nights').value = Math.max(0, nights);
        calculateBookingTotal();
    }
}

// --- 6. DASHBOARD & MANUAL BOOKING LOGIC ---

function initDashboard() {
    renderDashBookings();
    // In a real app, we'd load stats from Firebase here
}

function renderDashBookings() {
    const container = document.getElementById('dash-recent-bookings');
    if(!container) return;

    db.ref(`hotels/${hotelId}/bookings`).limitToLast(5).on('value', snap => {
        const rows = [];
        snap.forEach(child => {
            const b = child.val();
            rows.push(`
                <tr>
                    <td>${b.id.slice(-8)}</td>
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

    // Populate room types
    const typeSelect = document.getElementById('mb-room-type');
    const types = [...new Set(allRooms.map(r => r.roomType))];
    typeSelect.innerHTML = '<option value="">Select type</option>' + types.map(t => `<option value="${t}">${t}</option>`).join('');
};

window.closeManualBooking = () => {
    document.getElementById('manual-booking-modal').classList.add('hidden');
};

window.filterVacantRooms = () => {
    const type = document.getElementById('mb-room-type').value;
    const roomSelect = document.getElementById('mb-room-id');

    // Filter rooms that match type and are not occupied (using a mock check or property status)
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

window.toggleFullPay = () => {
    calculateBookingTotal();
};

window.submitManualBooking = () => {
    const guestName = document.getElementById('mb-guest-name').value;
    const roomId = document.getElementById('mb-room-id').value;

    if(!guestName || !roomId) {
        alert("Bhai, Guest Name aur Room toh bhar do!");
        return;
    }

    const checkInEl = document.getElementById('mb-check-in');
    const dates = checkInEl && checkInEl._flatpickr ? checkInEl._flatpickr.selectedDates : [];

    if (dates.length < 2) {
        alert("Bhai, Stay Dates toh sahi se select kar lo!");
        return;
    }

    const newBooking = {
        id: "MB-" + Date.now(),
        guestName: guestName,
        roomNumber: roomId,
        checkInDate: dates[0].toISOString(),
        checkOutDate: dates[1].toISOString(),
        totalAmount: parseFloat(document.getElementById('mb-total').value.replace(/,/g, '')),
        status: "Confirmed",
        timestamp: Date.now()
    };

    db.ref(`hotels/${hotelId}/bookings`).push(newBooking).then(() => {
        closeManualBooking();
        alert("Manual Booking Successful!");
    });
};

function initPickers() {
    let fromPicker;
    const untilPicker = flatpickr("#res-to-date", {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "Y-m-d",
        defaultDate: new Date().getTime() + 24 * 60 * 60 * 1000, // Next day
        onChange: function(selectedDates) {
            if (selectedDates.length === 1 && fromPicker) {
                const currentRange = fromPicker.selectedDates;
                if (currentRange.length > 0) {
                    fromPicker.setDate([currentRange[0], selectedDates[0]], false);
                }
            }
        }
    });

    fromPicker = flatpickr("#res-from-date", {
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
            // Override display to only show start date in the 'From' input
            if (selectedDates.length > 0) {
                const start = instance.formatDate(selectedDates[0], "Y-m-d");
                instance.altInput.value = start;
            }
        },
        onChange: function(selectedDates) {
            if (selectedDates.length === 2) {
                untilPicker.setDate(selectedDates[1], false);
            }
        }
    });
}

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

// --- 5. RENDER COMPONENTS (USING REUSABLE UI SYSTEM) ---
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
                    <td><span class="booking-id-link">${b.bookingNumber || b.id.slice(-8)}</span></td>
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

window.closeAddRoomModal = () => {
    document.getElementById('add-room-modal').classList.add('hidden');
};

function renderAddRoomForm() {
    const container = document.getElementById('add-room-form-container');
    if(!container) return;

    container.innerHTML = `
        <!-- Section 1: Please select -->
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
                    <label>Number of rooms (of this type)</label>
                    <input type="number" class="form-input-text" value="1" id="field-room-count">
                </div>
                <div class="form-group">
                    <label>Smoking policy</label>
                    <select class="form-select" id="field-smoking">
                        <option>Non-smoking</option>
                        <option>Smoking</option>
                        <option>Both available</option>
                    </select>
                </div>
            </div>
        </div>

        <!-- Section 2: Room location -->
        <div class="form-section-card">
            <h3>Room location</h3>
            <div class="form-group">
                <label>Floor Level</label>
                <select class="form-select">
                    <option>No selection</option>
                    <option>Ground floor</option>
                    <option>1st floor</option>
                    <option>2nd floor</option>
                </select>
                <p style="font-size:0.75rem; color:var(--text-muted); margin-top:8px;">
                    This helps your guests understand where the room is located.
                </p>
            </div>
        </div>

        <!-- Section 3: Bed options -->
        <div class="form-section-card">
            <h3>Bed options</h3>
            <div class="form-group">
                <label>What kind of beds are available in this room?</label>
                <div class="form-row-flex">
                    <select class="form-select" style="flex:2;">
                        <option>Double bed (131-150 cm wide)</option>
                        <option>Single bed (90-130 cm wide)</option>
                        <option>Extra-large double bed (181-210 cm wide)</option>
                    </select>
                    <div style="flex:1;">
                        <select class="form-select">
                            <option>1</option>
                            <option>2</option>
                        </select>
                    </div>
                </div>
            </div>
            <button class="btn btn-outline btn-sm" style="border-style:dashed;">+ Add another bed</button>
        </div>

        <!-- Section 4: Occupancy -->
        <div class="form-section-card">
            <h3>Occupancy</h3>
            <p style="font-size:0.8rem; color:var(--text-muted); margin-bottom:16px;">
                How many guests (adults and children) can stay here?
            </p>

            <div class="occupancy-row">
                <span>Maximum guests</span>
                <div class="number-picker">
                    <div class="picker-btn" onclick="updatePicker('max-guests', -1)">-</div>
                    <div class="picker-value" id="val-max-guests">2</div>
                    <div class="picker-btn" onclick="updatePicker('max-guests', 1)">+</div>
                </div>
            </div>

            <div class="occupancy-row">
                <span>Maximum adults</span>
                <div class="number-picker">
                    <div class="picker-btn" onclick="updatePicker('max-adults', -1)">-</div>
                    <div class="picker-value" id="val-max-adults">2</div>
                    <div class="picker-btn" onclick="updatePicker('max-adults', 1)">+</div>
                </div>
            </div>

            <div class="occupancy-row">
                <span>Maximum children</span>
                <div class="number-picker">
                    <div class="picker-btn" onclick="updatePicker('max-children', -1)">-</div>
                    <div class="picker-value" id="val-max-children">0</div>
                    <div class="picker-btn" onclick="updatePicker('max-children', 1)">+</div>
                </div>
            </div>
        </div>

        <!-- Section 5: Bathroom options -->
        <div class="form-section-card">
            <h3>Bathroom options</h3>
            <div class="form-group">
                <label>Is the bathroom private? (not shared with host or other guests)</label>
                <div class="radio-group">
                    <label class="radio-item"><input type="radio" name="private-bath" checked> Yes</label>
                    <label class="radio-item"><input type="radio" name="private-bath"> No</label>
                </div>
            </div>
        </div>
    `;
}

window.updatePicker = (id, delta) => {
    const el = document.getElementById(`val-${id}`);
    if(!el) return;
    let val = parseInt(el.innerText);
    val = Math.max(0, val + delta);
    el.innerText = val;
};

window.saveNewRoom = () => {
    const roomType = document.getElementById('field-room-type').value;
    if(roomType === 'Please select') {
        alert('Bhai, Room type toh select kar lo!');
        return;
    }

    const newRoom = {
        roomType: roomType,
        maxGuests: document.getElementById('val-max-guests').innerText,
        maxAdults: document.getElementById('val-max-adults').innerText,
        maxChildren: document.getElementById('val-max-children').innerText,
        id: "room_" + Math.random().toString(36).substr(2, 9),
        photos: []
    };

    // Save to Firebase (Realtime Sync will update the UI)
    db.ref(`hotels/${hotelId}/rooms`).push(newRoom).then(() => {
        closeAddRoomModal();
    });
};

function renderRoomDetails() {
    const container = document.getElementById('room-details-container');
    if(!container) return;

    let html = allRooms.map(room => `
        <div class="room-card">
            <div class="room-card-img-wrap">
                <img src="${room.photos && room.photos[0] ? room.photos[0] : 'https://i.ibb.co/6P0f9pL/ganga-homes-logo.jpg'}" alt="Room">
                <div class="room-card-info-bar">
                    <h4>${room.roomType}</h4>
                    <p>(${room.id || 'ID Pending'})</p>
                </div>
            </div>
            <div class="room-card-body">
                <div class="room-stat-line">Maximum guests: <b>${room.maxGuests} guests</b></div>
                <div class="room-stat-line">Maximum adults: <b>${room.maxAdults} adults</b></div>
                <div class="room-stat-line">Maximum children: <b>${room.maxChildren} children</b></div>
                <div class="room-stat-line">Number of this type: <b>1</b></div>
            </div>
            <div class="room-card-actions">
                <button class="btn btn-outline btn-sm">Edit</button>
                <button class="btn btn-outline btn-sm">Delete</button>
                <button class="btn btn-primary btn-sm">Upload photos</button>
            </div>
        </div>
    `).join('');

    // Add "Create New Room" card
    html += `
        <div class="create-room-card" onclick="openAddRoomModal()">
            <h3>Create a new room</h3>
            <div class="plus-circle">+</div>
        </div>
    `;

    container.innerHTML = html;
}

function renderRoomPhotoSections() {
    const container = document.getElementById('room-photos-container');
    if(!container) return;

    const distinctTypes = [...new Set(allRooms.map(r => r.roomType))];
    container.innerHTML = distinctTypes.map(type => {
        const photosHtml = `
            <div class="gallery-grid">
                ${[1,2,3].map(() => `<div class="photo-box"><img src="https://i.ibb.co/6P0f9pL/ganga-homes-logo.jpg"></div>`).join('')}
                <div class="photo-box photo-add-btn">+</div>
            </div>
        `;
        return UI.card(`${type} Gallery`, photosHtml, '<button class="btn btn-outline btn-sm">+ Add Photos</button>');
    }).join('');

    // Update main gallery too
    const mainGrid = document.getElementById('main-gallery-grid');
    if(mainGrid) {
        mainGrid.innerHTML = [1,2,3,4,5,6].map(() => `<div class="photo-box"><img src="https://i.ibb.co/6P0f9pL/ganga-homes-logo.jpg"></div>`).join('') + `<div class="photo-box photo-add-btn">+</div>`;
    }
}
