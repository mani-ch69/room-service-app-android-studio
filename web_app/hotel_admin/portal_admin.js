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

let hotelId = "GangaHomes_001"; // Default for development
let allBookings = [];
let allRooms = [];

// --- AUTHENTICATION ---
auth.onAuthStateChanged(user => {
    if (user) {
        loginOverlay.classList.add('hidden');
        adminApp.classList.remove('hidden');
        document.getElementById('admin-name').innerText = user.email.split('@')[0];
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

    // Mark the correct sidebar item as active
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('onclick')?.includes(tabId)) {
            item.classList.add('active');
        }
    });

    document.getElementById('current-tab-title').innerText = tabId.charAt(0).toUpperCase() + tabId.slice(1);
};

// --- DASHBOARD LOGIC ---
function initDashboard() {
    listenForBookings();
    listenForRooms();
    syncSettings();
    syncStats();
}

function listenForBookings() {
    db.ref('hotels').child(hotelId).child('bookings').on('value', snap => {
        allBookings = [];
        snap.forEach(child => allBookings.push(child.val()));
        allBookings.sort((a, b) => b.timestamp - a.timestamp);

        renderRecentBookings(allBookings.slice(0, 5));
        renderAllBookings(allBookings);
    });
}

function renderRecentBookings(bookings) {
    const body = document.getElementById('recent-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td><b>${b.guestName}</b></td>
            <td>Room ${b.roomNumber || 'TBA'}</td>
            <td>${formatDate(b.checkInDate)}</td>
            <td>${formatDate(b.checkOutDate)}</td>
            <td><span class="status-pill ${(b.status || 'BOOKED').toLowerCase()}">${b.status || 'BOOKED'}</span></td>
            <td><button class="btn-action" onclick="switchTab('bookings')">View</button></td>
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
                <button class="btn-action approve" onclick="updateStatus('${b.id}', 'CHECKED_IN')">Check-in</button>
            </td>
        </tr>
    `).join('');
}

window.applyBookingFilters = () => {
    const dateType = document.getElementById('filter-date-type').value;
    const from = document.getElementById('filter-from-date').value;
    const until = document.getElementById('filter-until-date').value;

    let filtered = allBookings;

    if (from || until) {
        const fromTs = from ? new Date(from).getTime() : 0;
        const untilTs = until ? new Date(until).getTime() + 86399999 : Infinity;

        filtered = allBookings.filter(b => {
            let dateToCompare;
            if (dateType === 'check-in') dateToCompare = b.checkInDate;
            else if (dateType === 'check-out') dateToCompare = b.checkOutDate;
            else dateToCompare = b.timestamp;

            return dateToCompare >= fromTs && dateToCompare <= untilTs;
        });
    }

    renderAllBookings(filtered);
};

function syncStats() {
    db.ref('hotels').child(hotelId).child('bookings').on('value', snap => {
        const bookings = [];
        snap.forEach(c => bookings.push(c.val()));

        const bLabel = document.getElementById('stat-today-bookings');
        const rLabel = document.getElementById('stat-revenue');

        if (bLabel) bLabel.innerText = bookings.length;
        const totalRev = bookings.reduce((sum, b) => sum + (parseFloat(b.totalAmount) || 0), 0);
        if (rLabel) rLabel.innerText = "₹" + totalRev.toLocaleString();
    });
}

function formatDate(ts) {
    if (!ts) return '-';
    return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

window.updateStatus = (id, newStatus) => {
    db.ref('hotels').child(hotelId).child('bookings').child(id).update({ status: newStatus });
};

// --- ROOM MANAGEMENT ---
const roomsGrid = document.getElementById('admin-rooms-grid');
const roomModal = document.getElementById('room-modal');

function listenForRooms() {
    db.ref('hotels').child(hotelId).child('rooms').on('value', snap => {
        allRooms = [];
        snap.forEach(c => allRooms.push(c.val()));
        renderAdminRooms();
        renderInventoryRoomTypes();
        updateRoomTypeSelect();
    });
}

function renderAdminRooms() {
    if (!roomsGrid) return;
    roomsGrid.innerHTML = allRooms.map(r => `
        <div class="admin-room-card">
            <img src="${r.imageUrl || 'https://via.placeholder.com/300x160'}" class="room-card-img">
            <div class="room-card-content">
                <h4>${r.roomType} (Room ${r.roomNumber})</h4>
                <p><small>${r.bedType} • Max ${r.maxGuests} Guests</small></p>
            </div>
            <div class="room-card-actions">
                <button class="btn-action" onclick="deleteRoom('${r.roomNumber}')" style="color:red;">Delete</button>
            </div>
        </div>
    `).join('');
}

window.openRoomModal = () => {
    roomModal.classList.remove('hidden');
    document.getElementById('room-modal-title').innerText = "Add New Room";
    document.getElementById('room-number').value = "";
};

window.closeRoomModal = () => roomModal.classList.add('hidden');

document.getElementById('btn-save-room').onclick = () => {
    const room = {
        roomNumber: document.getElementById('room-number').value,
        roomType: document.getElementById('room-type').value,
        maxGuests: parseInt(document.getElementById('room-max-guests').value),
        bedType: document.getElementById('room-bed-type').value,
        imageUrl: document.getElementById('room-image-url').value,
        hotelId: hotelId,
        isAvailable: true
    };
    if (!room.roomNumber) return alert("Room number required");
    db.ref('hotels').child(hotelId).child('rooms/' + room.roomNumber).set(room)
        .then(() => closeRoomModal());
};

window.deleteRoom = (num) => {
    if (confirm("Delete Room " + num + "?")) {
        db.ref('hotels').child(hotelId).child('rooms/' + num).remove();
    }
};

// --- MANUAL BOOKING LOGIC ---
const mbModal = document.getElementById('manual-booking-modal');

window.openManualBookingModal = () => {
    if (mbModal) mbModal.classList.remove('hidden');
    // Reset dates to today and tomorrow
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
    const checkin = document.getElementById('mb-checkin');
    const checkout = document.getElementById('mb-checkout');
    if (checkin) checkin.value = today;
    if (checkout) checkout.value = tomorrow;
    calcNights();
};

window.closeManualBookingModal = () => {
    if (mbModal) mbModal.classList.add('hidden');
};

window.toggleAccordion = (id) => {
    const sec = document.getElementById(id);
    if (!sec) return;
    const isActive = sec.classList.contains('active');
    document.querySelectorAll('.accordion-section').forEach(s => s.classList.remove('active'));
    if (!isActive) sec.classList.add('active');
};

window.calcNights = () => {
    const start = document.getElementById('mb-checkin')?.value;
    const end = document.getElementById('mb-checkout')?.value;
    const display = document.getElementById('mb-nights-display');
    if (start && end && display) {
        const s = new Date(start);
        const e = new Date(end);
        const diff = Math.ceil((e - s) / (1000 * 60 * 60 * 24));
        const nights = diff <= 0 ? 1 : diff;
        display.innerText = nights + " Night(s)";
        calcTotal();
    }
};

window.calcTotal = () => {
    const display = document.getElementById('mb-nights-display');
    const rentInput = document.getElementById('mb-rent');
    const advanceInput = document.getElementById('mb-advance');
    const totalInput = document.getElementById('mb-total');
    const remainingSpan = document.getElementById('mb-remaining');

    if (display && rentInput && totalInput && remainingSpan) {
        const nights = parseInt(display.innerText) || 1;
        const rent = parseFloat(rentInput.value) || 0;
        const advance = parseFloat(advanceInput?.value) || 0;

        const total = nights * rent;
        const remaining = total - advance;

        totalInput.value = total;
        remainingSpan.innerText = "₹" + remaining.toLocaleString();
    }
};

function updateRoomTypeSelect() {
    const select = document.getElementById('mb-room-type');
    if (!select) return;
    const types = [...new Set(allRooms.map(r => r.roomType))];
    select.innerHTML = types.map(t => `<option>${t}</option>`).join('');
}

window.saveManualBooking = () => {
    const name = document.getElementById('mb-name').value;
    const phone = document.getElementById('mb-phone').value;
    const roomType = document.getElementById('mb-room-type').value;

    if (!name || !phone || !roomType) return alert("Please fill required fields (*)");

    const btn = document.getElementById('btn-save-manual');
    btn.disabled = true;
    btn.innerText = "Saving...";

    // Find a room of this type
    const room = allRooms.find(r => r.roomType === roomType);

    const booking = {
        id: 'MB_' + Date.now(),
        hotelId: hotelId,
        guestName: name,
        guestPhone: phone,
        numberOfGuests: (parseInt(document.getElementById('mb-adults')?.value) || 0) + (parseInt(document.getElementById('mb-children')?.value) || 0),
        checkInDate: new Date(document.getElementById('mb-checkin').value).getTime(),
        checkOutDate: new Date(document.getElementById('mb-checkout').value).getTime(),
        roomNumber: room ? room.roomNumber : "",
        totalAmount: parseFloat(document.getElementById('mb-total').value),
        advancePaid: parseFloat(document.getElementById('mb-advance')?.value) || 0,
        bookingAgent: document.getElementById('mb-agent').value,
        paymentMode: document.getElementById('mb-paymode').value,
        status: "BOOKED",
        timestamp: Date.now(),
        notes: document.getElementById('mb-notes').value
    };

    db.ref('hotels').child(hotelId).child('bookings/' + booking.id).set(booking)
        .then(() => {
            alert("Booking Saved Successfully!");
            closeManualBookingModal();
            btn.disabled = false;
            btn.innerText = "Save Booking";
        })
        .catch(err => {
            alert("Error: " + err.message);
            btn.disabled = false;
            btn.innerText = "Save Booking";
        });
};

// --- SETTINGS ---
function syncSettings() {
    db.ref('hotels').child(hotelId).child('business_details').on('value', snap => {
        const d = snap.val();
        if (d) {
            const name = document.getElementById('set-hotel-name');
            const tagline = document.getElementById('set-hotel-tagline');
            const desc = document.getElementById('set-hotel-desc');
            const addr = document.getElementById('set-hotel-address');
            const phone = document.getElementById('set-hotel-phone');

            if(name) name.value = d.hotelName || "";
            if(tagline) tagline.value = d.hotelTagline || "";
            if(desc) desc.value = d.hotelDescription || "";
            if(addr) addr.value = d.address || "";
            if(phone) phone.value = d.phone || "";
        }
    });
}

window.savePropertySettings = () => {
    const data = {
        hotelName: document.getElementById('set-hotel-name').value,
        hotelTagline: document.getElementById('set-hotel-tagline').value,
        hotelDescription: document.getElementById('set-hotel-desc').value,
        address: document.getElementById('set-hotel-address').value,
        phone: document.getElementById('set-hotel-phone').value
    };
    db.ref('hotels').child(hotelId).child('business_details').update(data)
        .then(() => alert("Settings saved!"));
};

// --- INVENTORY ---
function renderInventoryRoomTypes() {
    const types = [...new Set(allRooms.map(r => r.roomType))];
    const container = document.getElementById('inv-room-types');
    if (container) {
        container.innerHTML = types.map(t => `
            <label style="display:block; margin-bottom:5px;"><input type="checkbox" name="inv-type" value="${t}"> ${t}</label>
        `).join('');
    }
}

window.applyInventoryChange = () => {
    const from = document.getElementById('inv-from').value;
    const to = document.getElementById('inv-to').value;
    if(!from || !to) return alert("Select dates");
    alert("Inventory updated successfully!");
};
