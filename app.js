// Firebase Configuration
const firebaseConfig = {
  apiKey: "AIzaSyBw6jDr8wRKeMMR7TiX8YiB0kO1wIfEmbE",
  authDomain: "roomserviceapk.firebaseapp.com",
  databaseURL: "https://roomserviceapk-default-rtdb.firebaseio.com",
  projectId: "roomserviceapk",
  storageBucket: "roomserviceapk.firebasestorage.app",
  messagingSenderId: "987842436715",
  appId: "1:987842436715:web:04d22839d4ca52c61e1b2e",
  measurementId: "G-8B9P993Z3Z"
};

// Initialize Firebase
try { firebase.initializeApp(firebaseConfig); } catch (e) { console.error("Firebase Init Error:", e); }
const db = firebase.database();
const auth = firebase.auth();

// Anonymous Login
auth.signInAnonymously().catch(e => console.error("Auth Error:", e));

// State
const urlParams = new URLSearchParams(window.location.search);
let hotelId = urlParams.get('hotel') || localStorage.getItem('hotel_id') || "GangaHomes_001";

// Persist for refresh
if (urlParams.has('hotel')) localStorage.setItem('hotel_id', hotelId);

let currentHotelData = null;
let allRooms = [];
let selectedRoomForBooking = null;

// UI Elements
const roomsGridEl = document.getElementById('rooms-grid');

// --- INITIALIZATION ---
function init() {
    syncBusinessDetails();
    syncRooms();
    setDefaultDates();
}

function syncBusinessDetails() {
    db.ref('hotels').child(hotelId).child('business_details').on('value', snap => {
        currentHotelData = snap.val();
        if (currentHotelData) {
            // Update names if provided in DB, otherwise use "Ganga Homes" as requested
            const name = currentHotelData.hotelName || "Ganga Homes";
            document.querySelectorAll('#hotel-name, .logo-text h1, .footer-logo h3').forEach(el => el.innerText = name.toUpperCase());
            document.title = name + " - Official Website";
        }
    });
}

function syncRooms() {
    console.log("Syncing Rooms for Hotel:", hotelId);
    db.ref('hotels').child(hotelId).child('rooms').on('value', snap => {
        allRooms = [];
        if (snap.exists()) {
            snap.forEach(child => {
                allRooms.push(child.val());
            });
        }
        renderRooms();
    });
}

function renderRooms() {
    if (!roomsGridEl) return;
    roomsGridEl.innerHTML = '';

    if (allRooms.length === 0) {
        roomsGridEl.innerHTML = `<div style="text-align:center; padding:60px; color:var(--text-light); grid-column: 1/-1;">No rooms available at the moment.</div>`;
        return;
    }

    allRooms.forEach(room => {
        const roomImg = room.imageUrl || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&q=80&w=800';
        const price = room.roomPrice || '2,500';

        roomsGridEl.innerHTML += `
            <div class="room-card">
                <div class="room-img-wrapper">
                    <img src="${roomImg}" alt="${room.roomType}">
                    ${room.isAvailable ? '' : '<div class="popular-badge" style="background:#666">Fully Booked</div>'}
                    ${room.roomNumber === "101" ? '<div class="popular-badge">Most Popular</div>' : ''}
                </div>
                <div class="room-info">
                    <h3>${room.roomType}</h3>
                    <div class="room-stats">
                        <span>👤 ${room.maxGuests} Adults</span>
                        <span>🛏️ ${room.bedType}</span>
                        <span>📏 ${room.roomSize || '250 sq.ft.'}</span>
                    </div>
                    <div class="room-icons">
                        <span>📶</span> <span>🛎️</span> <span>❄️</span> <span>🚿</span>
                    </div>
                    <div class="room-footer">
                        <div class="room-price-info">
                            <span class="price-tag">₹${price}</span>
                            <span class="price-unit">/ night</span>
                        </div>
                        <button class="btn-view-details" onclick="openBookingModal('${room.roomNumber}')">View Details</button>
                    </div>
                </div>
            </div>`;
    });
}

function setDefaultDates() {
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
    document.querySelectorAll('#hero-check-in, #check-in-date').forEach(el => el.value = today);
    document.querySelectorAll('#hero-check-out, #check-out-date').forEach(el => el.value = tomorrow);
}

// --- BOOKING MODAL ---
window.openBookingModal = (roomNumber) => {
    const room = allRooms.find(r => r.roomNumber === roomNumber);
    if (!room) return;
    selectedRoomForBooking = room;

    document.getElementById('modal-room-type').innerText = room.roomType;
    document.getElementById('booking-modal').classList.remove('hidden');
};

window.closeBookingModal = () => {
    document.getElementById('booking-modal').classList.add('hidden');
};

// --- CONFIRM BOOKING ---
document.getElementById('btn-confirm-booking').onclick = () => {
    const name = document.getElementById('guest-name').value.trim();
    const phone = document.getElementById('guest-phone').value.trim();
    const checkIn = document.getElementById('check-in-date').value;
    const checkOut = document.getElementById('check-out-date').value;

    if (!name || !phone || !checkIn || !checkOut) {
        alert("Please fill in all details to book.");
        return;
    }

    const bookingId = "BK" + Date.now().toString().slice(-6);
    const bookingData = {
        id: bookingId,
        bookingNumber: bookingId,
        hotelId: hotelId,
        roomNumber: selectedRoomForBooking.roomNumber,
        guestName: name,
        guestPhone: phone,
        checkInDate: new Date(checkIn).getTime(),
        checkOutDate: new Date(checkOut).getTime(),
        totalAmount: parseFloat(selectedRoomForBooking.roomPrice || 2500),
        advancePaid: 0,
        numberOfGuests: selectedRoomForBooking.maxGuests,
        status: "BOOKED",
        bookingAgent: "Website Visitor",
        timestamp: Date.now()
    };

    db.ref('hotels').child(hotelId).child('bookings').child(bookingId).set(bookingData)
        .then(() => {
            closeBookingModal();
            document.getElementById('success-booking-id').innerText = "#" + bookingId;
            document.getElementById('success-modal').classList.remove('hidden');
            // Reset form
            document.getElementById('guest-name').value = '';
            document.getElementById('guest-phone').value = '';
        })
        .catch(err => {
            console.error("Booking Error:", err);
            alert("Sorry, booking failed. Please try again later.");
        });
};

// Start
init();
