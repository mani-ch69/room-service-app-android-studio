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

    // Find the clicked item and mark it active
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        if (item.innerText.toLowerCase().includes(tabId)) {
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
        const bookings = [];
        snap.forEach(child => bookings.push(child.val()));
        bookings.sort((a, b) => b.timestamp - a.timestamp);

        renderRecentBookings(bookings.slice(0, 5));
        renderAllBookings(bookings);
    });
}

function renderRecentBookings(bookings) {
    const body = document.getElementById('recent-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td><b>${b.guestName}</b></td>
            <td>Room ${b.roomNumber}</td>
            <td>${formatDate(b.checkInDate)}</td>
            <td>${formatDate(b.checkOutDate)}</td>
            <td><span class="status-pill ${b.status.toLowerCase()}">${b.status}</span></td>
            <td><button class="btn-action" onclick="switchTab('bookings')">View</button></td>
        </tr>
    `).join('');
}

function renderAllBookings(bookings) {
    const body = document.getElementById('all-bookings-body');
    if (!body) return;
    body.innerHTML = bookings.map(b => `
        <tr>
            <td>#${b.id.slice(-6)}</td>
            <td><b>${b.guestName}</b><br><small>${b.guestPhone}</small></td>
            <td>${b.roomNumber}</td>
            <td>${formatDate(b.checkInDate)} - ${formatDate(b.checkOutDate)}</td>
            <td>₹${b.totalAmount}</td>
            <td><span class="status-pill ${b.status.toLowerCase()}">${b.status}</span></td>
            <td>
                <button class="btn-action approve" onclick="updateStatus('${b.id}', 'CHECKED_IN')">Check-in</button>
            </td>
        </tr>
    `).join('');
}

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
    return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

window.updateStatus = (id, newStatus) => {
    db.ref('hotels').child(hotelId).child('bookings').child(id).update({ status: newStatus });
};

// --- ROOM MANAGEMENT ---
let allRooms = [];
const roomsGrid = document.getElementById('admin-rooms-grid');
const roomModal = document.getElementById('room-modal');

function listenForRooms() {
    db.ref('hotels').child(hotelId).child('rooms').on('value', snap => {
        allRooms = [];
        snap.forEach(c => allRooms.push(c.val()));
        renderAdminRooms();
        renderInventoryRoomTypes();
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
            <label><input type="checkbox" name="inv-type" value="${t}"> ${t}</label>
        `).join('');
    }
}

window.applyInventoryChange = () => {
    const from = document.getElementById('inv-from').value;
    const to = document.getElementById('inv-to').value;
    if(!from || !to) return alert("Select dates");
    alert("Inventory logic applied to selected dates.");
};
