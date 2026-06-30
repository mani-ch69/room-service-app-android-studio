// Firebase Configuration
const firebaseConfig = {
  apiKey: "AIzaSyBw6jDr8wRKeMMR7TiX8YiB0kO1wIfEmbE",
  authDomain: "roomserviceapk.firebaseapp.com",
  databaseURL: "https://roomserviceapk-default-rtdb.firebaseio.com",
  projectId: "roomserviceapk",
  storageBucket: "roomserviceapk.firebasestorage.app",
  messagingSenderId: "987842436715",
  appId: "1:987842436715:web:04d22839d4ca52c61e1b2e"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.database();
const auth = firebase.auth();

// UI Elements
const loginOverlay = document.getElementById('login-overlay');
const adminApp = document.getElementById('admin-app');
const btnLogin = document.getElementById('btn-login');
const btnLogout = document.getElementById('btn-logout');
const loginError = document.getElementById('login-error');

let hotelId = "GangaHomes_001";
let allBookings = [];
let allRooms = [];

// --- AUTHENTICATION ---
auth.onAuthStateChanged(user => {
    if (user) {
        hotelId = user.uid; // Dynamically set hotelId to user UID
        loginOverlay.classList.add('hidden');
        adminApp.classList.remove('hidden');
        const adminName = user.email.split('@')[0].toUpperCase();
        document.getElementById('sidebar-user-name').innerText = adminName;
        document.getElementById('header-greeting').innerText = `Welcome back, ${adminName}!`;
        initDashboard();
    } else {
        loginOverlay.classList.remove('hidden');
        adminApp.classList.add('hidden');
    }
});

btnLogin.onclick = () => {
    const email = document.getElementById('admin-email').value;
    const password = document.getElementById('admin-password').value;
    auth.signInWithEmailAndPassword(email, password)
        .catch(err => {
            loginError.innerText = err.message;
            loginError.classList.remove('hidden');
        });
};

btnLogout.onclick = () => auth.signOut();

// --- NAVIGATION ---
window.switchTab = (tabId) => {
    document.querySelectorAll('.page-content').forEach(p => p.classList.add('hidden'));
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));

    const target = document.getElementById('tab-' + tabId);
    if (target) target.classList.remove('hidden');

    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('onclick')?.includes(tabId)) {
            item.classList.add('active');
        }
    });
};

// --- DASHBOARD LOGIC ---
function initDashboard() {
    listenForBookings();
    listenForRooms();
    syncSettings();
    updateDateDisplay();
}

function updateDateDisplay() {
    const now = new Date();
    const options = { day: '2-digit', month: 'short', year: 'numeric' };
    document.getElementById('current-date-display').innerText = now.toLocaleDateString('en-GB', options);
}

function listenForBookings() {
    db.ref('hotels').child(hotelId).child('bookings').on('value', snap => {
        allBookings = [];
        snap.forEach(child => allBookings.push(child.val()));
        allBookings.sort((a, b) => b.timestamp - a.timestamp);

        renderDashboardStats();
        renderRecentBookings(allBookings.slice(0, 5));
        renderAllBookings(allBookings);
    });
}

function renderDashboardStats() {
    const today = new Date().setHours(0,0,0,0);
    const todayBookings = allBookings.filter(b => isSameDay(b.checkInDate, today));
    const todayCheckouts = allBookings.filter(b => isSameDay(b.checkOutDate, today));

    document.getElementById('stat-total-bookings').innerText = allBookings.length;
    document.getElementById('stat-checkins').innerText = todayBookings.length;
    document.getElementById('stat-checkouts').innerText = todayCheckouts.length;

    const revenue = allBookings.reduce((sum, b) => sum + (parseFloat(b.totalAmount) || 0), 0);
    document.getElementById('rev-month').innerText = "₹" + revenue.toLocaleString();
    document.getElementById('stat-total-guests').innerText = allBookings.reduce((sum, b) => sum + (parseInt(b.numberOfGuests) || 0), 0);
}

function isSameDay(d1, d2) {
    return new Date(d1).setHours(0,0,0,0) === new Date(d2).setHours(0,0,0,0);
}

function renderRecentBookings(bookings) {
    const body = document.getElementById('recent-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td style="color:var(--primary); font-family:monospace; font-size:0.8rem;">#REF_${(b.id || '').slice(-6).toUpperCase()}</td>
            <td>
                <div style="font-weight:800;">${b.guestName}</div>
                <div style="font-size:0.7rem; color:#888;">${b.numberOfGuests} PAX Portfolio</div>
            </td>
            <td style="font-weight:800;">Unit ${b.roomNumber || 'TBA'}</td>
            <td style="font-size:0.8rem;">
                <span style="color:#059669;">In: ${formatDate(b.checkInDate)}</span><br>
                <span style="color:#DC2626;">Out: ${formatDate(b.checkOutDate)}</span>
            </td>
            <td style="font-weight:800;">₹${(b.totalAmount || 0).toLocaleString()}</td>
            <td>
                <span class="status-tag ${b.status === 'CHECKED_IN' ? 'tag-checkin' : (b.status === 'BOOKED' ? 'tag-confirmed' : 'tag-upcoming')}">
                    ${b.status || 'BOOKED'}
                </span>
            </td>
        </tr>
    `).join('');
}

function renderAllBookings(bookings) {
    const body = document.getElementById('all-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td style="color:var(--primary); font-family:monospace; font-size:0.8rem;">#REF_${(b.id || '').slice(-6).toUpperCase()}</td>
            <td><b>${b.guestName}</b><br><small>${b.guestPhone || ''}</small></td>
            <td>${b.roomNumber || 'TBA'}</td>
            <td>${formatDate(b.checkInDate)} - ${formatDate(b.checkOutDate)}</td>
            <td>₹${b.totalAmount || 0}</td>
            <td><span class="status-pill ${(b.status || 'BOOKED').toLowerCase()}">${b.status || 'BOOKED'}</span></td>
            <td>
                <button class="btn-action approve" onclick="updateStatus('${b.id}', 'CHECKED_IN')" style="border: 1px solid #eee; background: white; padding: 4px 8px; border-radius: 4px; cursor: pointer;">Check-in</button>
            </td>
        </tr>
    `).join('');
}

function formatDate(ts) {
    if (!ts) return '-';
    return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

window.updateStatus = (id, newStatus) => {
    db.ref('hotels').child(hotelId).child('bookings').child(id).update({ status: newStatus });
};

// --- ROOM MANAGEMENT ---
function listenForRooms() {
    db.ref('hotels').child(hotelId).child('rooms').on('value', snap => {
        allRooms = [];
        snap.forEach(c => allRooms.push(c.val()));
        renderAdminRooms();
        renderDashboardRoomGrid();
        updateRoomTypeSelect();
        renderInventoryRoomTypes();
    });
}

function renderDashboardRoomGrid() {
    const grid = document.getElementById('dashboard-rooms-grid');
    if (!grid) return;
    grid.innerHTML = allRooms.slice(0, 8).map(r => `
        <div class="room-unit ${r.isAvailable ? 'available' : 'occupied'}">
            ${r.roomNumber}
            <span>${r.isAvailable ? 'AVAILABLE' : 'RESERVED'}</span>
        </div>
    `).join('');
}

function formatRoomId(roomNumber) {
    if (!roomNumber) return "0000000000";
    let hash = 0;
    for (let i = 0; i < roomNumber.length; i++) {
        hash = ((hash << 5) - hash) + roomNumber.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash).toString().padStart(10, '0').slice(-10);
}

function renderAdminRooms() {
    const roomsGrid = document.getElementById('admin-rooms-grid');
    if (!roomsGrid) return;
    roomsGrid.innerHTML = allRooms.map(r => `
        <div class="card-panel" style="padding:0; overflow:hidden; border: 1px solid #eee;">
            <div style="height: 140px; background: #f8fafc; display: flex; align-items: center; justify-content: center; font-size: 3rem; position: relative; border-bottom: 1px solid #f1f5f9;">
                ${r.imageUrl ? `<img src="${r.imageUrl}" style="width:100%; height:100%; object-fit:cover;">` : '🛏️'}
                <div style="position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.6); color: white; padding: 8px 12px; text-align: left;">
                    <div style="font-weight: 800; font-size: 0.9rem;">${r.roomType}</div>
                    <div style="font-size: 0.7rem; opacity: 0.9;">Room ID: ${formatRoomId(r.roomNumber)}</div>
                </div>
            </div>
            <div style="padding: 16px; text-align: left;">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 0.75rem; color: #64748b; font-weight: 600;">
                    <div>Max Guests: <span style="color:black;">${r.maxGuests || 2}</span></div>
                    <div>Max Adults: <span style="color:black;">${r.maxAdults || 2}</span></div>
                    <div>Max Children: <span style="color:black;">${r.maxChildren || 0}</span></div>
                    <div>Count: <span style="color:black;">${r.totalUnits || 1}</span></div>
                </div>
                <div style="margin-top: 12px; border-top: 1px solid #f1f5f9; padding-top: 12px; display: flex; justify-content: space-between; align-items: center;">
                    <div style="font-size: 0.7rem; font-weight: 800; color: #64748b;">Number of this type: <span style="color:var(--primary);">${r.totalUnits || 1}</span></div>
                    <div style="display: flex; gap: 8px;">
                        <button onclick="deleteRoom('${r.roomNumber}')" style="background:none; border:none; color:#ef4444; font-size:0.8rem; cursor:pointer;">🗑️</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

window.openRoomModal = () => document.getElementById('room-modal').classList.remove('hidden');
window.closeRoomModal = () => document.getElementById('room-modal').classList.add('hidden');

window.saveRoom = () => {
    const type = document.getElementById('rm-type').value;
    const units = parseInt(document.getElementById('rm-units').value) || 1;
    const smoking = document.getElementById('rm-smoking').value;
    const floor = document.getElementById('rm-floor').value;
    const bedType = document.getElementById('rm-bed-type').value;
    const bedCount = parseInt(document.getElementById('rm-bed-count').value) || 1;
    const maxGuests = parseInt(document.getElementById('rm-max-guests').value) || 2;
    const maxAdults = parseInt(document.getElementById('rm-max-adults').value) || 2;
    const maxChildren = parseInt(document.getElementById('rm-max-children').value) || 0;
    const bathCount = parseInt(document.getElementById('rm-bath-count').value) || 1;

    if (!type) return alert("Room Type is mandatory");

    const room = {
        roomNumber: type, // Using type as key for parity with current app logic
        roomType: type,
        totalUnits: units,
        smokingPolicy: smoking,
        floorLevel: floor,
        bedType: bedType,
        numberOfBeds: bedCount,
        maxGuests: maxGuests,
        maxAdults: maxAdults,
        maxChildren: maxChildren,
        numBathrooms: bathCount,
        hotelId: hotelId,
        isAvailable: true,
        imageUrl: ""
    };

    db.ref('hotels').child(hotelId).child('rooms').child(room.roomNumber).set(room)
        .then(() => {
            alert("Room added successfully!");
            closeRoomModal();
        })
        .catch(err => {
            console.error("Save error:", err);
            alert("Error saving room: " + err.message);
        });
};

window.deleteRoom = (roomNumber) => {
    if(confirm(`Are you sure you want to delete room ${roomNumber}?`)) {
        db.ref('hotels').child(hotelId).child('rooms').child(roomNumber).remove();
    }
};

// --- MANUAL BOOKING ---
const mbModal = document.getElementById('manual-booking-modal');
window.openManualBookingModal = () => {
    mbModal.classList.remove('hidden');
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
    document.getElementById('mb-checkin').value = today;
    document.getElementById('mb-checkout').value = tomorrow;
    calcNights();
};
window.closeManualBookingModal = () => mbModal.classList.add('hidden');

window.calcNights = () => {
    const start = document.getElementById('mb-checkin').value;
    const end = document.getElementById('mb-checkout').value;
    if (start && end) {
        const diff = Math.ceil((new Date(end) - new Date(start)) / (1000 * 60 * 60 * 24));
        window.currentNights = diff <= 0 ? 1 : diff;
        calcTotal();
    }
};

window.calcTotal = () => {
    const rent = parseFloat(document.getElementById('mb-rent').value) || 0;
    const total = (window.currentNights || 1) * rent;
    document.getElementById('mb-total-display').innerText = "₹" + total.toLocaleString();
};

function updateRoomTypeSelect() {
    const select = document.getElementById('mb-room-type');
    if (!select) return;
    const types = [...new Set(allRooms.map(r => r.roomType))];
    select.innerHTML = types.length ? types.map(t => `<option>${t}</option>`).join('') : '<option>No rooms available</option>';
}

window.saveManualBooking = () => {
    const name = document.getElementById('mb-name').value;
    const phone = document.getElementById('mb-phone').value;
    const roomType = document.getElementById('mb-room-type').value;
    const rent = parseFloat(document.getElementById('mb-rent').value) || 0;

    if (!name || !phone) return alert("Required Metadata Missing");

    const room = allRooms.find(r => r.roomType === roomType);
    const booking = {
        id: 'BK_' + Date.now(),
        hotelId: hotelId,
        guestName: name,
        guestPhone: phone,
        numberOfGuests: 2,
        checkInDate: new Date(document.getElementById('mb-checkin').value).getTime(),
        checkOutDate: new Date(document.getElementById('mb-checkout').value).getTime(),
        roomNumber: room ? room.roomNumber : "TBA",
        totalAmount: (window.currentNights || 1) * rent,
        status: "BOOKED",
        timestamp: Date.now()
    };

    db.ref('hotels').child(hotelId).child('bookings/' + booking.id).set(booking)
        .then(() => {
            alert("Authorization Successful - Record Committed");
            closeManualBookingModal();
        });
};

// --- SETTINGS & INVENTORY ---
function syncSettings() {
    db.ref('hotels').child(hotelId).child('business_details').on('value', snap => {
        const d = snap.val();
        if (d) {
            document.getElementById('set-hotel-name').value = d.hotelName || "";
            document.getElementById('set-hotel-address').value = d.address || "";
        }
    });
}

window.savePropertySettings = () => {
    const data = {
        hotelName: document.getElementById('set-hotel-name').value,
        address: document.getElementById('set-hotel-address').value
    };
    db.ref('hotels').child(hotelId).child('business_details').update(data).then(() => alert("Profile Synchronized"));
};

function renderInventoryRoomTypes() {
    const container = document.getElementById('inv-room-types');
    if (!container) return;
    const types = [...new Set(allRooms.map(r => r.roomType))];
    container.innerHTML = types.map(t => `<label><input type="checkbox" value="${t}"> ${t}</label>`).join('');
}

window.toggleMoreFilters = () => document.getElementById('more-filters').classList.toggle('hidden');

window.applyBookingFilters = () => {
    const from = document.getElementById('filter-from-date').value;
    const until = document.getElementById('filter-until-date').value;
    const search = document.getElementById('filter-search-advanced').value.toLowerCase();

    let filtered = allBookings;
    if (from) filtered = filtered.filter(b => b.checkInDate >= new Date(from).getTime());
    if (until) filtered = filtered.filter(b => b.checkInDate <= new Date(until).getTime());
    if (search) filtered = filtered.filter(b => b.guestName.toLowerCase().includes(search) || b.id.toLowerCase().includes(search));

    renderAllBookings(filtered);
};
