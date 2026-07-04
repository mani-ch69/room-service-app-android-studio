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
let allRooms = [];
let allBookings = [];

// --- 2. CORE NAVIGATION ---
window.switchTab = (tabId) => {
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.add('hidden'));
    const target = document.getElementById(`tab-${tabId}`);
    if(target) target.classList.remove('hidden');

    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    // Handle both direct onclick and event-based calls
    if(event) event.currentTarget.classList.add('active');

    if(tabId === 'reservations') renderReservations();
};

window.switchSubTab = (subId) => {
    document.querySelectorAll('.sub-pane').forEach(p => p.classList.add('hidden'));
    document.getElementById(`sub-${subId}`).classList.remove('hidden');
    document.querySelectorAll('.sub-tab').forEach(t => t.classList.remove('active'));
    event.currentTarget.classList.add('active');
};

// --- 3. DATA SYNC ---
window.onload = () => {
    startRealtimeSync();
    renderAmenities();
};

function startRealtimeSync() {
    db.ref(`hotels/${hotelId}/business_details`).on('value', snap => {
        const d = snap.val();
        if(d) {
            document.getElementById('nav-hotel-name').innerText = d.hotelName.toUpperCase();
            document.getElementById('disp-hotel-name').innerText = d.hotelName;
            document.getElementById('disp-hotel-addr').innerText = d.address;
        }
    });

    db.ref(`hotels/${hotelId}/rooms`).on('value', snap => {
        allRooms = [];
        snap.forEach(c => allRooms.push(c.val()));
        renderRoomPhotoSections();
    });
}

// --- 4. RENDER COMPONENTS ---
function renderReservations() {
    const container = document.getElementById('reservations-table-body');
    if(!container) return;

    db.ref(`hotels/${hotelId}/bookings`).on('value', snap => {
        let html = '';
        snap.forEach(child => {
            const b = child.val();
            const cin = new Date(b.checkInDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
            const cout = new Date(b.checkOutDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
            const bookedOn = b.timestamp ? new Date(b.timestamp).toLocaleDateString('en-GB', { day:'2-digit', month:'short' }) : '-';

            html += `
                <tr>
                    <td><span class="guest-name-link">${b.guestName}</span></td>
                    <td>${cin}</td>
                    <td>${cout}</td>
                    <td>1 × ${b.roomNumber || 'Deluxe'}</td>
                    <td>${bookedOn}</td>
                    <td><span class="status-tag ${(b.status || 'OK').toLowerCase()}">${b.status || 'OK'}</span></td>
                    <td>₹ ${(b.totalAmount || 0).toLocaleString()}</td>
                    <td>₹ 0</td>
                    <td><span class="booking-id-link">${b.bookingNumber || b.id.slice(-8)}</span></td>
                </tr>
            `;
        });
        container.innerHTML = html || '<tr><td colspan="9" style="text-align:center;">No reservations found</td></tr>';
    });
}
function renderAmenities() {
    const amenities = ["Air conditioning", "Balcony", "View", "Flat-screen TV", "Terrace", "Electric kettle", "Toilet paper", "Towels", "Linens"];
    const container = document.getElementById('amenities-list');
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

function renderRoomPhotoSections() {
    const container = document.getElementById('room-photos-container');
    const distinctTypes = [...new Set(allRooms.map(r => r.roomType))];
    container.innerHTML = distinctTypes.map(type => `
        <div class="card-ui">
            <div class="card-head">${type} <button class="btn btn-outline btn-sm">+ Add Photos</button></div>
            <div class="card-body">
                <div class="gallery-grid">
                    ${[1,2,3].map(() => `<div class="photo-box"><img src="https://i.ibb.co/Xf7yZ8N/gh-stay-logo.jpg"></div>`).join('')}
                    <div class="photo-box photo-add-btn">+</div>
                </div>
            </div>
        </div>
    `).join('');

    // Update main gallery too
    const mainGrid = document.getElementById('main-gallery-grid');
    mainGrid.innerHTML = [1,2,3,4,5,6].map(() => `<div class="photo-box"><img src="https://i.ibb.co/Xf7yZ8N/gh-stay-logo.jpg"></div>`).join('') + `<div class="photo-box photo-add-btn">+</div>`;
}
