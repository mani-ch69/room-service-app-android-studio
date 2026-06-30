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
        loginOverlay.classList.add('hidden');
        adminApp.classList.remove('hidden');
        const adminName = user.email.split('@')[0];
        document.getElementById('sidebar-user-name').innerText = adminName.charAt(0).toUpperCase() + adminName.slice(1);
        document.getElementById('header-greeting').innerText = "Welcome back, " + adminName.charAt(0).toUpperCase() + adminName.slice(1) + "!";
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
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.add('hidden'));
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));

    const target = document.getElementById('tab-' + tabId);
    if (target) target.classList.remove('hidden');

    document.querySelectorAll('.nav-item').forEach(item => {
        const onclick = item.getAttribute('onclick') || '';
        if (onclick.includes(tabId)) {
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
    const today = new Date().setHours(0, 0, 0, 0);
    const todayBookings = allBookings.filter(b => new Date(b.checkInDate).setHours(0, 0, 0, 0) === today);
    const todayCheckouts = allBookings.filter(b => new Date(b.checkOutDate).setHours(0, 0, 0, 0) === today);

    document.getElementById('stat-total-bookings').innerText = allBookings.length;
    document.getElementById('stat-checkins').innerText = todayBookings.length;
    document.getElementById('stat-checkouts').innerText = todayCheckouts.length;

    const revenue = allBookings.reduce((sum, b) => sum + (parseFloat(b.totalAmount) || 0), 0);
    document.getElementById('rev-month').innerText = "₹" + revenue.toLocaleString();
    document.getElementById('stat-total-guests').innerText = allBookings.reduce((sum, b) => sum + (parseInt(b.numberOfGuests) || 0), 0);
}

function renderRecentBookings(bookings) {
    const body = document.getElementById('recent-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td>#${(b.id || '').slice(-6)}</td>
            <td><b>${b.guestName}</b></td>
            <td>Room ${b.roomNumber || 'TBA'}</td>
            <td>${formatDate(b.checkInDate)}</td>
            <td>${formatDate(b.checkOutDate)}</td>
            <td><span class="status-pill ${(b.status || 'BOOKED').toLowerCase()}">${b.status || 'BOOKED'}</span></td>
        </tr>
    `).join('');
}

function renderAllBookings(bookings) {
    const body = document.getElementById('all-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td>#${(b.id || '').slice(-6)}</td>
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
    return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
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
    // Show first 8-12 rooms on dashboard
    grid.innerHTML = allRooms.slice(0, 8).map(r => `
        <div class="room-box ${r.isAvailable ? 'available' : 'occupied'}">
            <span>${r.roomNumber}</span>
            ${r.isAvailable ? 'Available' : 'Occupied'}
        </div>
    `).join('');
}

function renderAdminRooms() {
    const roomsGrid = document.getElementById('admin-rooms-grid');
    if (!roomsGrid) return;
    roomsGrid.innerHTML = allRooms.map(r => `
        <div class="card" style="padding: 15px;">
            <div style="height: 120px; background: #f0f0f0; border-radius: 8px; margin-bottom: 12px; display: flex; align-items: center; justify-content: center; font-size: 2rem;">🛏️</div>
            <h4>${r.roomType} - Room ${r.roomNumber}</h4>
            <p style="font-size: 0.8rem; color: #7f8c8d; margin: 5px 0;">Max Guests: ${r.maxGuests || 2}</p>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                <span class="status-pill ${r.isAvailable ? 'confirmed' : 'checked_in'}">${r.isAvailable ? 'Available' : 'Occupied'}</span>
                <button onclick="deleteRoom('${r.roomNumber}')" style="color: #e74c3c; background: none; border: none; cursor: pointer; font-size: 0.8rem; font-weight: 700;">Delete</button>
            </div>
        </div>
    `).join('');
}

window.openRoomModal = () => {
    const modal = document.getElementById('room-modal');
    if (!modal) return;
    document.getElementById('rm-room-number').value = '';
    document.getElementById('rm-room-type').value = 'Single';
    document.getElementById('rm-floor-level').value = 'Ground floor';
    document.getElementById('rm-total-units').value = 1;
    document.getElementById('rm-smoking-policy').value = 'Non-smoking';
    document.getElementById('rm-max-guests').value = 2;
    document.getElementById('rm-max-adults').value = 2;
    document.getElementById('rm-max-children').value = 0;
    document.getElementById('rm-bed-type').value = 'King';
    document.getElementById('rm-number-of-beds').value = 1;
    document.getElementById('rm-bathroom-private').value = 'yes';
    document.getElementById('rm-image-url').value = '';
    document.getElementById('rm-available').value = 'true';
    modal.classList.remove('hidden');
};

window.closeRoomModal = () => {
    const modal = document.getElementById('room-modal');
    if (modal) modal.classList.add('hidden');
};

function saveRoom() {
    const roomNumber = document.getElementById('rm-room-number').value.trim();
    const roomType = document.getElementById('rm-room-type').value.trim();
    if (!roomNumber || !roomType) {
        return alert('Room number and room type are required.');
    }

    const roomData = {
        roomNumber,
        roomType,
        floorLevel: document.getElementById('rm-floor-level').value || 'Ground floor',
        totalUnits: parseInt(document.getElementById('rm-total-units').value) || 1,
        maxGuests: parseInt(document.getElementById('rm-max-guests').value) || 2,
        maxAdults: parseInt(document.getElementById('rm-max-adults').value) || 2,
        maxChildren: parseInt(document.getElementById('rm-max-children').value) || 0,
        bedType: document.getElementById('rm-bed-type').value || 'King',
        numberOfBeds: parseInt(document.getElementById('rm-number-of-beds').value) || 1,
        imageUrl: document.getElementById('rm-image-url').value || '',
        isAvailable: document.getElementById('rm-available').value === 'true',
        hotelId: hotelId,
        qrToken: 'QR_' + Date.now(),
        smokingPolicy: document.getElementById('rm-smoking-policy').value || 'Non-smoking',
        numBathrooms: 1,
        isBathroomPrivate: document.getElementById('rm-bathroom-private').value === 'yes',
        roomSize: '250 sqft',
        hasAc: true,
        isBathroomInside: true,
        hasGeyser: true,
        hasKettle: true,
        timestamp: Date.now()
    };

    db.ref('hotels').child(hotelId).child('rooms').child(roomNumber).set(roomData)
        .then(() => {
            alert('Room added successfully.');
            closeRoomModal();
        })
        .catch(err => {
            console.error('Room save failed', err);
            alert('Could not add room. Please try again.');
        });
}

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

    if (!name || !phone) return alert("Fill required fields");

    const room = allRooms.find(r => r.roomType === roomType);
    const booking = {
        id: 'BK' + Date.now().toString().slice(-8),
        hotelId: hotelId,
        guestName: name,
        guestPhone: phone,
        numberOfGuests: parseInt(document.getElementById('mb-adults').value) || 2,
        checkInDate: new Date(document.getElementById('mb-checkin').value).getTime(),
        checkOutDate: new Date(document.getElementById('mb-checkout').value).getTime(),
        roomNumber: room ? room.roomNumber : "TBA",
        totalAmount: (window.currentNights || 1) * rent,
        status: "BOOKED",
        timestamp: Date.now()
    };

    db.ref('hotels').child(hotelId).child('bookings/' + booking.id).set(booking)
        .then(() => {
            alert("Booking Confirmed!");
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
            document.getElementById('set-hotel-desc').value = d.hotelDescription || "";
        }
    });
}

window.savePropertySettings = () => {
    const data = {
        hotelName: document.getElementById('set-hotel-name').value,
        address: document.getElementById('set-hotel-address').value,
        hotelDescription: document.getElementById('set-hotel-desc').value
    };
    db.ref('hotels').child(hotelId).child('business_details').update(data).then(() => alert("Saved!"));
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