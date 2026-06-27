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

    document.getElementById('tab-' + tabId).classList.remove('hidden');
    event.currentTarget.classList.add('active');
    document.getElementById('current-tab-title').innerText = tabId.charAt(0).toUpperCase() + tabId.slice(1);
};

// --- DASHBOARD LOGIC ---
function initDashboard() {
    listenForBookings();
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
    body.innerHTML = bookings.map(b => `
        <tr>
            <td><b>${b.guestName}</b></td>
            <td>Room ${b.roomNumber}</td>
            <td>${formatDate(b.checkInDate)}</td>
            <td>${formatDate(b.checkOutDate)}</td>
            <td><span class="status-pill ${b.status.toLowerCase()}">${b.status}</span></td>
            <td><button class="btn-action" onclick="viewBooking('${b.id}')">Details</button></td>
        </tr>
    `).join('');
}

function renderAllBookings(bookings) {
    const body = document.getElementById('all-bookings-body');
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

        document.getElementById('stat-today-bookings').innerText = bookings.length;
        const totalRev = bookings.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
        document.getElementById('stat-revenue').innerText = "₹" + totalRev.toLocaleString();
    });
}

function formatDate(ts) {
    return new Date(ts).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

// --- GLOBALS FOR UI ---
window.updateStatus = (id, newStatus) => {
    db.ref('hotels').child(hotelId).child('bookings').child(id).update({ status: newStatus });
};
