// --- 1. REUSABLE UI COMPONENTS (THE "COMPONENTS" SYSTEM) ---
const UI = {
    card: (title, content, action = '') => `
        <div class="card-ui">
            <div class="card-head">
                <span>${title}</span>
                ${action ? `<div class="card-action">${action}</div>` : ''}
            </div>
            <div class="card-body">${content}</div>
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
  appId: "1:987842436715:web:04d22839d4ca52c61e1b2e"
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

window.switchSubTab = (subId) => {
    document.querySelectorAll('.sub-pane').forEach(p => p.classList.add('hidden'));
    const target = document.getElementById(`sub-${subId}`);
    if(target) target.classList.remove('hidden');

    document.querySelectorAll('.sub-tab').forEach(t => t.classList.remove('active'));

    // Highlight the correct sub-tab if not called from click
    const activeSubTab = Array.from(document.querySelectorAll('.sub-tab')).find(t => t.onclick && t.onclick.toString().includes(`'${subId}'`));
    if(activeSubTab) activeSubTab.classList.add('active');
    else if(window.event && window.event.currentTarget) window.event.currentTarget.classList.add('active');

    if(subId === 'score') renderPropertyScore();
    if(subId === 'property-policies') renderPropertyPolicies();
    if(subId === 'reservation-policies') renderReservationPolicies();
    if(subId === 'descriptions') renderDescriptions();
    if(subId === 'sustainability') renderSustainability();
};

// --- 4. DATA SYNC ---
window.onload = () => {
    startRealtimeSync();
    renderAmenities();
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

// --- 6. NEW FEATURE RENDERING ---

function renderPropertyScore() {
    const container = document.getElementById('score-container');
    if(!container) return;

    container.innerHTML = `
        <div class="score-flex">
            <div class="score-circle-wrap">
                <svg width="120" height="120" viewBox="0 0 120 120">
                    <circle cx="60" cy="60" r="54" fill="none" stroke="#F1F5F9" stroke-width="12" />
                    <circle cx="60" cy="60" r="54" fill="none" stroke="var(--primary)" stroke-width="12"
                            stroke-dasharray="339.292" stroke-dashoffset="84.823" stroke-linecap="round" />
                </svg>
                <div class="score-number" style="position:absolute;">75%</div>
            </div>
            <div class="score-details">
                <h3>Your property page score is good!</h3>
                <p style="color:var(--text-muted); margin-bottom:16px;">Properties with a 100% score get up to 18% more bookings.</p>
                <div class="score-tip-item">
                    <div class="score-tip-text">Add 3 more high-quality photos</div>
                    <button class="btn btn-primary btn-sm">+4%</button>
                </div>
                <div class="score-tip-item">
                    <div class="score-tip-text">Update your breakfast options</div>
                    <button class="btn btn-primary btn-sm">+2%</button>
                </div>
                <div class="score-tip-item">
                    <div class="score-tip-text">Respond to all pending reviews</div>
                    <button class="btn btn-primary btn-sm">+5%</button>
                </div>
            </div>
        </div>
    `;
}

function renderPropertyPolicies() {
    const container = document.getElementById('property-policies-container');
    if(!container) return;

    container.innerHTML = `
        <div class="page-header">
            <h3>Property policies</h3>
            <p>All your property-related policy info is here.</p>
        </div>

        ${UI.infoBanner('You can create even more tailored child rates with the new flexible child rates.')}

        <div class="policy-card-modern">
            <div class="policy-card-head">Children policies</div>
            <div class="policy-card-body">
                <div class="policy-subsection">
                    <h6>Child policies</h6>
                    <ul class="policy-bullet-list">
                        <li>Children 17 and older are allowed.</li>
                    </ul>
                </div>
                <div class="policy-subsection">
                    <h6>Children rates</h6>
                    <ul class="policy-bullet-list">
                        <li>Children 17 years old can stay for 10.00% of the adult price per child, per night.</li>
                    </ul>
                </div>
                <button class="btn btn-primary btn-sm">Edit</button>
            </div>
        </div>

        <div class="policy-card-modern">
            <div class="policy-card-head">Extra bed & crib options</div>
            <div class="policy-card-body">
                <div class="policy-subsection">
                    <h6>Cribs</h6>
                    <ul class="policy-bullet-list">
                        <li>You haven't added any cribs.</li>
                    </ul>
                </div>
                <div class="policy-subsection">
                    <h6>Extra beds</h6>
                    <ul class="policy-bullet-list">
                        <li>You haven't added any extra beds.</li>
                    </ul>
                </div>
                <button class="btn btn-primary btn-sm">Edit</button>
            </div>
        </div>
    `;
}

function renderReservationPolicies() {
    const container = document.getElementById('reservation-policies-container');
    if(!container) return;

    container.innerHTML = `
        <div class="page-header">
            <h3>Reservation policies</h3>
            <p>Manage your cancellation and prepayment policies.</p>
        </div>

        <div class="policy-card-modern" style="display:flex;">
            <div class="policy-card-body" style="flex:1;">
                <h6>Flexible - 7 days (General)</h6>
                <ul class="policy-bullet-list" style="margin-top:12px;">
                    <li>The guest can cancel free of charge until 7 days before arrival. The guest will be charged the cost of the first night if they cancel within 7 days of arrival.</li>
                    <li>No prepayment is needed.</li>
                </ul>
                <div style="display:flex; gap:8px; margin-top:20px;">
                    <button class="btn btn-primary btn-sm">Edit</button>
                    <button class="btn btn-outline btn-sm">Apply to other properties</button>
                </div>
            </div>
            <div class="policy-report-panel">
                <div class="report-item">
                    <div class="report-label">Report from</div>
                    <div class="report-value">Apr 4, 2024 to Jul 3, 2024</div>
                </div>
                <div class="report-item">
                    <div class="report-label">Total Room Nights</div>
                    <div class="report-value">12</div>
                </div>
                <div class="report-item">
                    <div class="report-label">Total Revenue</div>
                    <div class="report-value">INR 7,127.78</div>
                </div>
            </div>
        </div>

        <div class="policy-card-modern">
            <div class="policy-card-body">
                <h6>Non-refundable (Non Refundable)</h6>
                <ul class="policy-bullet-list" style="margin-top:12px;">
                    <li>The guest will be charged the total price of the reservation if they cancel anytime.</li>
                    <li>The guest will be charged a prepayment of the total price of the reservation at any time.</li>
                </ul>
                <button class="btn btn-danger btn-sm" style="margin-top:20px;">Delete</button>
            </div>
        </div>
    `;
}

function renderDescriptions() {
    const container = document.getElementById('descriptions-container');
    if(!container) return;

    container.innerHTML = `
        <div class="desc-box">
            <h6>Property Description</h6>
            <div class="desc-text">Located in the heart of Varanasi, Ganga Homes offers peaceful accommodation with stunning views of the Ganges. Just steps away from Assi Ghat, our property provides a unique spiritual experience combined with modern comfort.</div>
        </div>
        <div class="desc-box">
            <h6>Deluxe Double Room Description</h6>
            <div class="desc-text">This air-conditioned room features a flat-screen TV, electric kettle and private bathroom with shower. The balcony offers views of the city or river.</div>
        </div>
        <button class="btn btn-primary">Request Description Edit</button>
    `;
}

function renderSustainability() {
    const container = document.getElementById('sustainability-container');
    if(!container) return;

    const items = [
        "No single-use plastic water bottles",
        "No single-use plastic straws",
        "Water-efficient toilets",
        "Water-efficient showers",
        "Option to opt-out of daily room cleaning",
        "Energy-efficient LED light bulbs",
        "Most food provided is locally sourced",
        "100% renewable electricity used throughout"
    ];

    container.innerHTML = `
        <p style="margin-bottom:20px; color:var(--text-muted);">Show guests what you're doing to be more sustainable.</p>
        ${items.map(item => `
            <div class="sustainability-item">
                <input type="checkbox" checked>
                <span style="font-size:0.9rem;">${item}</span>
            </div>
        `).join('')}
        <button class="btn btn-primary" style="margin-top:24px;">Save Changes</button>
    `;
}
