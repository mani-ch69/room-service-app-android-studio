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
let hotelId = urlParams.get('hotel') || localStorage.getItem('hotel_id') || "DEMO_HOTEL";

// Persist for refresh
if (urlParams.has('hotel')) localStorage.setItem('hotel_id', hotelId);

let currentHotelData = null;
let currentMainTab = 'rooms';
let allRooms = [];
let selectedRoomForBooking = null;

// UI Elements
const hotelLogo = document.getElementById('hotel-logo');
const popupHotelName = document.getElementById('popup-hotel-name');
const popupHotelId = document.getElementById('popup-hotel-id');
const optionsPopup = document.getElementById('options-popup');
const btnMoreMenu = document.getElementById('btn-more-menu');
const roomsListEl = document.getElementById('rooms-list');
const infoDescription = document.getElementById('info-description');
const infoAddress = document.getElementById('info-address');
const infoContact = document.getElementById('info-contact');

// --- INITIALIZATION ---
function init() {
    updateHeader();
    syncBusinessDetails();
    syncRooms();
    setupPopup();
}

function updateHeader() {
    const shortId = hotelId.slice(-6).toUpperCase();
    if (popupHotelId) popupHotelId.innerText = `ID: ${shortId}`;
}

function setupPopup() {
    if (!btnMoreMenu || !optionsPopup) return;
    btnMoreMenu.onclick = (e) => {
        e.stopPropagation();
        optionsPopup.classList.toggle('hidden');
    };
    document.addEventListener('click', () => {
        optionsPopup.classList.add('hidden');
    });
}

function syncBusinessDetails() {
    db.ref('hotels').child(hotelId).child('business_details').on('value', snap => {
        currentHotelData = snap.val();
        if (currentHotelData) {
            const name = currentHotelData.hotelName || "Hotel Service";
            if (hotelLogo) hotelLogo.innerText = name;
            if (popupHotelName) popupHotelName.innerText = name;
            if (infoDescription) infoDescription.innerText = currentHotelData.hotelDescription || "Welcome to our premium hotel service.";
            if (infoAddress) infoAddress.innerText = currentHotelData.address || "Address not provided";
            if (infoContact) infoContact.innerText = currentHotelData.phone || "Contact not provided";

            const taglineEl = document.getElementById('hero-hotel-tagline');
            if (taglineEl) taglineEl.innerText = currentHotelData.hotelTagline || "Your perfect stay awaits. Book directly for best rates.";
            const heroNameEl = document.getElementById('hero-hotel-name');
            if (heroNameEl) heroNameEl.innerText = currentHotelData.heroTitle || "Experience Luxury";

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
    if (!roomsListEl) return;
    roomsListEl.innerHTML = '';

    if (allRooms.length === 0) {
        roomsListEl.innerHTML = `<div style="text-align:center; padding:60px; color:var(--text-light);">No rooms available at the moment.</div>`;
        return;
    }

    allRooms.forEach(room => {
        const roomImg = room.imageUrl || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&q=80&w=800';
        roomsListEl.innerHTML += `
            <div class="room-card">
                <div class="room-img-container">
                    <img src="${roomImg}" class="room-card-img" alt="${room.roomType}">
                    <div class="room-badge">${room.roomType}</div>
                </div>
                <div class="room-card-info">
                    <h3>${room.roomType} (Room ${room.roomNumber})</h3>
                    <div class="room-amenities">
                        <span class="amenity-chip">Max ${room.maxGuests} Guests</span>
                        <span class="amenity-chip">${room.bedType}</span>
                        ${room.isBathroomPrivate ? '<span class="amenity-chip">Private Bath</span>' : ''}
                    </div>
                    <div class="room-card-footer">
                        <div class="room-price">
                            <span class="price-label">Price per night</span>
                            <span class="price-value">₹${room.roomPrice || '1,500'}</span>
                        </div>
                        <button class="book-btn" onclick="openBookingModal('${room.roomNumber}')">BOOK NOW</button>
                    </div>
                </div>
            </div>`;
    });
}

// --- BOOKING MODAL ---
window.openBookingModal = (roomNumber) => {
    const room = allRooms.find(r => r.roomNumber === roomNumber);
    if (!room) return;
    selectedRoomForBooking = room;

    document.getElementById('modal-room-type').innerText = room.roomType;
    document.getElementById('summary-price').innerText = `₹${room.roomPrice || '1,500'}`;

    // Set default dates
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
    document.getElementById('check-in-date').value = today;
    document.getElementById('check-out-date').value = tomorrow;

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
        totalAmount: parseFloat(selectedRoomForBooking.roomPrice || 1500),
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

// --- TABS & SECTIONS ---
window.switchMainTab = (tab) => {
    currentMainTab = tab;
    document.getElementById('tab-rooms').classList.toggle('active', tab === 'rooms');
    document.getElementById('tab-info').classList.toggle('active', tab === 'info');
    document.getElementById('section-rooms').classList.toggle('hidden', tab !== 'rooms');
    document.getElementById('section-info').classList.toggle('hidden', tab !== 'info');
    document.getElementById('hero-section').classList.toggle('hidden', tab !== 'rooms');

    // Sync Bottom Nav
    document.getElementById('nav-home').classList.toggle('active', tab === 'rooms');
    document.getElementById('nav-rooms').classList.toggle('active', tab === 'rooms');
    document.getElementById('nav-info').classList.toggle('active', tab === 'info');
};

window.switchSection = (section) => {
    if (section === 'home' || section === 'rooms') switchMainTab('rooms');
    if (section === 'info') switchMainTab('info');
};

// Start
init();
