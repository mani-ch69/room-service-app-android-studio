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
let roomNumber = (urlParams.get('room') || localStorage.getItem('hotel_room_number') || "Guest").trim();

// Persist for refresh
if (urlParams.has('hotel')) localStorage.setItem('hotel_id', hotelId);
if (urlParams.has('room')) localStorage.setItem('hotel_room_number', roomNumber);

let currentHotelData = null;
let currentMainTab = 'restaurant';
let currentCategory = '';
let allMenuItems = [];
let allCategories = [];
let cart = {};

// UI Elements
const popupHotelName = document.getElementById('popup-hotel-name');
const popupHotelId = document.getElementById('popup-hotel-id');
const optionsPopup = document.getElementById('options-popup');
const btnMoreMenu = document.getElementById('btn-more-menu');
const resCategoriesEl = document.getElementById('res-categories');
const resMenuListEl = document.getElementById('res-menu-list');
const ordersTimelineList = document.getElementById('orders-timeline-list');
const chatMessagesContainer = document.getElementById('chat-messages-container');
const chatInput = document.getElementById('chat-input');
const btnSendChat = document.getElementById('btn-send-chat');

// --- INITIALIZATION ---
function init() {
    updateHeader();
    syncBusinessDetails();
    syncMenu();
    setupPopup();
    listenForOrders();
    listenForMessages();
    setupChat();
}

function updateHeader() {
    const shortId = hotelId.slice(-6).toUpperCase();
    if (popupHotelId) popupHotelId.innerText = `ID: ${shortId}`;
    const popupRoomEl = document.getElementById('popup-room-number');
    if (popupRoomEl) popupRoomEl.innerText = `Room ${roomNumber}`;
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
            if (popupHotelName) popupHotelName.innerText = currentHotelData.hotelName || "Hotel Service";
            document.title = (currentHotelData.hotelName || "Room Service") + " - Guest Portal";
        }
    });
}

function syncMenu() {
    console.log("Starting Menu Sync for Hotel:", hotelId);
    const hotelRef = db.ref('hotels').child(hotelId);

    let officialCategories = [];
    let itemCategories = new Set();

    const updateMenuUI = () => {
        const categoriesMap = new Map();

        // 1. Process official categories from Admin
        officialCategories.forEach(cat => {
            const name = (typeof cat === 'string') ? cat : (cat.name || "");
            if (name.trim()) {
                categoriesMap.set(name.trim().toLowerCase(), name.trim());
            }
        });

        // 2. Process categories found in items (as fallback)
        itemCategories.forEach(name => {
            if (name && name.trim()) {
                const lower = name.trim().toLowerCase();
                if (!categoriesMap.has(lower)) {
                    categoriesMap.set(lower, name.trim());
                }
            }
        });

        // 3. If still empty but we have items, create an "All" category
        if (categoriesMap.size === 0 && allMenuItems.length > 0) {
            categoriesMap.set("all items", "All Items");
        }

        allCategories = Array.from(categoriesMap.values()).map(name => ({ name }));
        console.log("Final Categories List:", allCategories);

        if (allCategories.length > 0) {
            const stillValid = allCategories.some(c => c.name.toLowerCase() === (currentCategory || "").toLowerCase());
            if (!currentCategory || !stillValid) {
                currentCategory = allCategories[0].name;
            }
        }

        renderCategories();
        renderMenu();
    };

    // Official Categories Listener
    hotelRef.child('menu/categories').on('value', snap => {
        officialCategories = [];
        if (snap.exists()) {
            snap.forEach(child => {
                officialCategories.push(child.val());
            });
        }
        console.log("Received Official Categories:", officialCategories);
        updateMenuUI();
    });

    // Menu Items Listener
    hotelRef.child('menu/items').on('value', snap => {
        allMenuItems = [];
        itemCategories.clear();
        if (snap.exists()) {
            snap.forEach(child => {
                const item = child.val();
                if (item.isAvailable !== false) {
                    allMenuItems.push(item);
                    if (item.category) itemCategories.add(item.category);
                }
            });
        }
        console.log("Received Menu Items:", allMenuItems.length);
        updateMenuUI();
    }, error => {
        console.error("Firebase Menu Read Error:", error);
    });
}

function renderCategories() {
    if (!resCategoriesEl) return;
    let html = '';
    allCategories.forEach(cat => {
        const isActive = (currentCategory || "").toLowerCase() === cat.name.toLowerCase();
        html += `<div class="category-tab ${isActive ? 'active' : ''}" onclick="filterByCategory('${cat.name}')">${cat.name.toUpperCase()}</div>`;
    });
    if (allCategories.length === 0) {
        html = '<p style="font-size:0.8rem; color:var(--text-light); padding:0 20px;">No categories found.</p>';
    }
    resCategoriesEl.innerHTML = html;
}

window.filterByCategory = (catName) => {
    currentCategory = catName;
    renderCategories();
    renderMenu();
};

// --- MENU RENDERING ---
function renderMenu() {
    if (!resMenuListEl) return;
    resMenuListEl.innerHTML = '';

    const lowerCurrentCat = (currentCategory || "").toLowerCase();
    const itemsToShow = allMenuItems.filter(i =>
        (i.category || "").toLowerCase() === lowerCurrentCat ||
        lowerCurrentCat === "all items"
    );

    if (itemsToShow.length === 0) {
        resMenuListEl.innerHTML = `
            <div style="text-align:center; padding:60px 20px; color:var(--text-light);">
                <div style="font-size:40px; margin-bottom:10px;">🍽️</div>
                <p>No items found in <b>${currentCategory || 'this category'}</b></p>
            </div>`;
        return;
    }

    itemsToShow.forEach(item => {
        const qty = cart[item.id]?.qty || 0;
        resMenuListEl.innerHTML += `
            <div class="menu-item-card">
                <img src="${item.imageUrl || 'https://via.placeholder.com/100'}" class="item-img">
                <div class="item-info" style="flex:1">
                    <h3>${item.name}</h3>
                    <p class="item-desc">${item.description || 'Premium quality choice.'}</p>
                    <div class="item-footer">
                        <div class="item-price">₹${item.price}</div>
                        <div class="action-area">
                            ${qty > 0 ? `
                                <div class="qty-control">
                                    <button class="qty-btn" onclick="updateQty('${item.id}', -1)">-</button>
                                    <span style="font-weight:800; min-width:20px; text-align:center;">${qty}</span>
                                    <button class="qty-btn" onclick="updateQty('${item.id}', 1)">+</button>
                                </div>
                            ` : `
                                <button class="add-btn" onclick="updateQty('${item.id}', 1)">ADD</button>
                            `}
                        </div>
                    </div>
                </div>
            </div>`;
    });
}

// --- CART LOGIC ---
window.updateQty = (id, delta) => {
    if (!cart[id] && delta > 0) {
        const item = allMenuItems.find(i => i.id === id);
        if (item) cart[id] = { item, qty: 1 };
    } else if (cart[id]) {
        cart[id].qty += delta;
        if (cart[id].qty <= 0) delete cart[id];
    }
    renderMenu();
    updateCartBar();

    // If cart modal is open, refresh it
    if (!document.getElementById('cart-modal').classList.contains('hidden')) {
        showCartModal();
    }
};

function updateCartBar() {
    const bar = document.getElementById('floating-cart-bar');
    const badge = document.getElementById('cart-badge');
    const items = Object.values(cart);
    const count = items.reduce((s, c) => s + c.qty, 0);
    const total = items.reduce((s, c) => s + (c.item.price * c.qty), 0);

    if (count > 0) {
        bar.classList.remove('hidden');
        if (badge) { badge.classList.remove('hidden'); badge.innerText = count; }
        document.getElementById('cart-item-count').innerText = `${count} Items`;
        document.getElementById('cart-total-price').innerText = `₹${total}`;
    } else {
        bar.classList.add('hidden');
        if (badge) badge.classList.add('hidden');
        // Close modal if empty
        document.getElementById('cart-modal').classList.add('hidden');
    }
}

// --- CART MODAL ---
if (document.getElementById('btn-cart')) document.getElementById('btn-cart').onclick = () => showCartModal();
if (document.getElementById('btn-go-to-cart')) document.getElementById('btn-go-to-cart').onclick = () => showCartModal();
if (document.getElementById('btn-close-cart')) document.getElementById('btn-close-cart').onclick = () => document.getElementById('cart-modal').classList.add('hidden');

function showCartModal() {
    const items = Object.values(cart);
    if (items.length === 0) {
        document.getElementById('cart-modal').classList.add('hidden');
        return;
    }
    const list = document.getElementById('cart-items-list');
    list.innerHTML = items.map(c => `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; padding: 10px; background: #F8FAFC; border-radius: 12px;">
            <div style="flex:1">
                <h4 style="font-weight:700; font-size:0.9rem;">${c.item.name}</h4>
                <p style="font-size:0.8rem; color:var(--text-medium);">₹${c.item.price}</p>
            </div>
            <div style="display:flex; align-items:center; gap:12px;">
                <div class="cart-qty-control">
                    <button class="cart-qty-btn" onclick="updateQty('${c.item.id}', -1)">-</button>
                    <span style="font-weight:800; min-width:15px; text-align:center; font-size:0.9rem;">${c.qty}</span>
                    <button class="cart-qty-btn" onclick="updateQty('${c.item.id}', 1)">+</button>
                </div>
                <div style="font-weight:800; color:var(--primary-blue); min-width: 60px; text-align: right;">₹${c.item.price * c.qty}</div>
            </div>
        </div>
    `).join('');
    const subtotal = items.reduce((s, c) => s + (c.item.price * c.qty), 0);
    const tax = Math.round(subtotal * 0.05);
    document.getElementById('bill-subtotal').innerText = `₹${subtotal}`;
    document.getElementById('bill-tax').innerText = `₹${tax}`;
    document.getElementById('bill-grand-total').innerText = `₹${subtotal + tax}`;
    document.getElementById('cart-modal').classList.remove('hidden');
}

// --- ORDER PLACEMENT ---
if (document.getElementById('btn-place-final-order')) {
    document.getElementById('btn-place-final-order').onclick = () => {
        if (roomNumber === "Guest") { alert("Please scan Room QR to place order."); return; }
        const items = Object.values(cart);
        const subtotal = items.reduce((s, c) => s + (c.item.price * c.qty), 0);

        // Get selected payment method
        const payMethods = document.getElementsByName('pay-method');
        let selectedPayMethod = "Cash on Delivery";
        for(let i=0; i<payMethods.length; i++) {
            if(payMethods[i].checked) {
                selectedPayMethod = payMethods[i].value;
                break;
            }
        }

        if (!hotelId || hotelId === "DEMO_HOTEL") {
            alert("Warning: No specific Hotel ID detected. Order may not reach the intended Admin.");
        }

        const orderId = "ORD" + Date.now().toString().slice(-6);
        const taxAmount = Math.round(subtotal * 0.05);
        const totalAmount = subtotal + taxAmount;

        const orderData = {
            id: orderId,
            roomNumber: roomNumber.toString(),
            status: "PENDING",
            timestamp: Date.now(),
            items: items.map(c => ({
                name: c.item.name || "Unknown Item",
                quantity: parseInt(c.qty) || 1,
                price: parseFloat(c.item.price) || 0
            })),
            subtotal: parseFloat(subtotal) || 0,
            tax: parseFloat(taxAmount) || 0,
            totalAmount: parseFloat(totalAmount) || 0,
            paymentMethod: selectedPayMethod,
            deliveryPin: "",
            notes: "",
            assignedStaffId: "",
            assignedStaffName: ""
        };

        console.log("Attempting to place order:", orderData);

        db.ref('hotels').child(hotelId).child('orders/' + orderId).set(orderData)
            .then(() => {
                console.log("Order recorded successfully in Firebase!");
                // Send Chat Message for the new order
                const messageId = db.ref('hotels').child(hotelId).child('messages').push().key;
                const itemsList = items.map(c => `${c.qty}x ${c.item.name}`).join(', ');
                const orderMessage = `📦 New Order Placed: #${orderId.slice(-6)}\nItems: ${itemsList}\nTotal: ₹${totalAmount}\nPayment: ${selectedPayMethod}`;

                db.ref('hotels').child(hotelId).child('messages/' + messageId).set({
                    id: messageId,
                    roomNumber: roomNumber,
                    text: orderMessage,
                    senderId: roomNumber,
                    timestamp: Date.now()
                });

                cart = {};
                updateCartBar();
                renderMenu();
                document.getElementById('cart-modal').classList.add('hidden');
                document.getElementById('success-order-id').innerText = "#" + orderId.slice(-6);
                document.getElementById('success-modal').classList.remove('hidden');
            })
            .catch(error => {
                console.error("Firebase Order Save Error:", error);
                alert("Order failed to record: " + error.message);
            });
    };
}

if (document.getElementById('btn-success-close')) {
    document.getElementById('btn-success-close').onclick = () => document.getElementById('success-modal').classList.add('hidden');
}

// --- ORDERS SECTION ---
function listenForOrders() {
    db.ref('hotels').child(hotelId).child('orders')
        .orderByChild('roomNumber').equalTo(roomNumber)
        .on('value', snap => {
            renderOrdersTimeline(snap);
        });
}

function renderOrdersTimeline(snap) {
    const listContainer = document.getElementById('orders-timeline-list');
    if (!listContainer) return;
    if (!snap.exists()) {
        listContainer.innerHTML = `<div style="text-align:center; padding:60px 20px; color:var(--text-light);"><div style="font-size:50px; margin-bottom:16px;">📑</div><p style="font-weight:600;">No orders found for Room ${roomNumber}</p></div>`;
        return;
    }
    let html = '';
    const orders = [];
    snap.forEach(c => orders.push(c.val()));
    orders.sort((a, b) => b.timestamp - a.timestamp);
    orders.forEach(order => {
        const shortId = order.id.slice(-6).toUpperCase();
        const dateObj = new Date(order.timestamp);
        const timePlaced = dateObj.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const datePlaced = dateObj.toLocaleDateString([], { day: '2-digit', month: 'short' });
        const steps = [
            { label: 'Order Placed', time: timePlaced, active: true },
            { label: 'Accepted', time: order.acceptedTime || '', active: order.status !== 'PENDING' && order.status !== 'CANCELLED' },
            { label: 'Preparing', time: '', active: ['PROCESSING', 'READY', 'PICKED_UP', 'DELIVERED'].includes(order.status) },
            { label: 'Ready', time: order.readyTime || '', active: ['READY', 'PICKED_UP', 'DELIVERED'].includes(order.status) },
            { label: 'Out for Delivery', time: order.pickupTime || '', active: ['PICKED_UP', 'DELIVERED'].includes(order.status) },
            { label: 'Delivered', time: order.deliveredTime || '', active: order.status === 'DELIVERED' }
        ];
        html += `
            <div class="card" style="padding:20px; margin-bottom:12px; border-left: 5px solid ${order.status === 'DELIVERED' ? 'var(--accent-green)' : 'var(--primary-blue)'};">
                <div style="display:flex; justify-content:space-between; align-items:start; margin-bottom:12px;">
                    <div><h3 style="font-size:1.1rem; font-weight:900; color:var(--text-dark);">Order #${shortId}</h3><p style="font-size:0.75rem; color:var(--text-medium); font-weight:600;">${datePlaced} • ${order.items.length} Items</p></div>
                    <div style="text-align:right;"><span style="background:var(--bg-light); padding:5px 12px; border-radius:10px; font-size:0.7rem; font-weight:900; color:var(--primary-blue); border:1px solid #E2E8F0;">${order.status}</span></div>
                </div>
                <div style="font-size:0.85rem; color:var(--text-medium); margin-bottom:16px; padding:10px; background:#F8FAFC; border-radius:8px;">
                    ${order.items.map(i => `<div style="display:flex; justify-content:space-between;"><span>${i.quantity}x ${i.name}</span><b>₹${i.price * i.quantity}</b></div>`).join('')}
                    <div style="margin-top:8px; padding-top:8px; border-top:1px dashed #CBD5E1; display:flex; justify-content:space-between; color:var(--text-dark); font-weight:800;"><span>Total Paid</span><span>₹${order.totalAmount}</span></div>
                </div>
                ${order.assignedStaffName ? `<div style="display:flex; align-items:center; gap:10px; margin-bottom:16px; padding:10px; background:rgba(25,118,210,0.05); border-radius:10px; border:1px solid rgba(25,118,210,0.1);"><div style="width:32px; height:32px; background:var(--primary-blue); color:white; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:14px;">👤</div><div style="flex:1"><div style="font-size:0.75rem; font-weight:800; color:var(--text-dark);">${order.assignedStaffName}</div><div style="font-size:0.65rem; color:var(--text-medium);">Assigned Staff</div></div>${order.assignedStaffPhone ? `<a href="tel:${order.assignedStaffPhone}" style="padding:8px 12px; background:var(--accent-green); color:white; border-radius:8px; text-decoration:none; font-size:0.7rem; font-weight:800;">📞 CALL</a>` : ''}</div>` : ''}
                <div class="timeline">${steps.map(s => `<div class="timeline-item"><div class="timeline-dot ${s.active ? 'active' : ''}"></div><div class="timeline-content"><span class="timeline-title" style="color: ${s.active ? 'var(--text-dark)' : 'var(--text-light)'}">${s.label}</span><span class="timeline-time">${s.time}</span></div></div>`).join('')}</div>
                ${order.deliveryPin && order.status !== 'DELIVERED' && order.status !== 'CANCELLED' ? `<div class="pin-display-card"><p style="font-size:0.7rem; font-weight:800; letter-spacing:1px; margin-bottom:5px;">SHARE THIS PIN WITH STAFF</p><div class="pin-code">${order.deliveryPin}</div></div>` : ''}
            </div>`;
    });
    listContainer.innerHTML = html;
}

// --- CHAT SECTION ---
function listenForMessages() {
    if (!chatMessagesContainer) return;
    db.ref('hotels').child(hotelId).child('messages')
        .orderByChild('roomNumber').equalTo(roomNumber)
        .on('value', snap => {
            const messages = [];
            snap.forEach(c => messages.push(c.val()));
            messages.sort((a, b) => a.timestamp - b.timestamp);

            chatMessagesContainer.innerHTML = messages.map(m => {
                const isAdmin = m.senderId === 'admin';
                const time = new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                return `
                    <div class="chat-bubble ${isAdmin ? 'admin' : 'guest'}">
                        ${m.text}
                        <span class="chat-time">${time}</span>
                    </div>
                `;
            }).join('');

            // Auto scroll to bottom
            chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
        });
}

function setupChat() {
    if (!btnSendChat || !chatInput) return;
    const sendMessage = () => {
        const text = chatInput.value.trim();
        if (!text) return;
        if (roomNumber === "Guest") { alert("Please scan Room QR to chat."); return; }

        const messageId = db.ref('hotels').child(hotelId).child('messages').push().key;
        db.ref('hotels').child(hotelId).child('messages/' + messageId).set({
            id: messageId,
            roomNumber: roomNumber,
            text: text,
            senderId: roomNumber,
            timestamp: Date.now()
        }).then(() => {
            chatInput.value = '';
        });
    };

    btnSendChat.onclick = sendMessage;
    chatInput.onkeypress = (e) => { if (e.key === 'Enter') sendMessage(); };
}

// --- TABS & SECTIONS ---
window.switchMainTab = (tab) => {
    currentMainTab = tab;
    document.getElementById('tab-restaurant').classList.toggle('active', tab === 'restaurant');
    document.getElementById('tab-shop').classList.toggle('active', tab === 'shop');
    document.getElementById('section-restaurant').classList.toggle('hidden', tab !== 'restaurant');
    document.getElementById('section-shop').classList.toggle('hidden', tab !== 'shop');
    if (tab === 'restaurant') switchSection('restaurant');
};

window.switchSection = (section) => {
    ['section-restaurant', 'section-shop', 'section-orders', 'section-messages'].forEach(s => {
        const el = document.getElementById(s);
        if (el) el.classList.add('hidden');
    });
    const activeEl = document.getElementById('section-' + (section === 'home' ? 'restaurant' : section));
    if (activeEl) activeEl.classList.remove('hidden');
    document.querySelectorAll('.nav-item').forEach((nav, idx) => {
        const sections = ['home', 'orders', 'messages'];
        nav.classList.toggle('active', sections[idx] === section);
    });
};

// Start
init();
