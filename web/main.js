// --- CONFIGURATION & INITIALIZATION ---

// IMPORTANT: Do not hardcode Firebase config in production apps.
// Use environment variables or a secure configuration service.
const firebaseConfig = {
    databaseURL: "https://staffcontrolapp-default-rtdb.firebaseio.com/"
};
firebase.initializeApp(firebaseConfig);
const db = firebase.database();

// --- STATE ---

const urlParams = new URLSearchParams(window.location.search);
const roomNum = urlParams.get('room') || '1';
let fullMenu = [];
let cart = {}; // Use an object to easily manage quantities
let activeCategory = "All";

// --- UI ELEMENTS ---

const roomNumLabel = document.getElementById('roomNumLabel');
const callBtn = document.getElementById('call-btn');
const categoryListEl = document.getElementById('category-list');
const menuGridEl = document.getElementById('menu-grid');
const cartSheet = document.getElementById('cartSheet');
const itemCountEl = document.getElementById('itemCount');
const totalPriceEl = document.getElementById('totalPrice');
const orderBtn = document.getElementById('order-btn');

// --- FUNCTIONS ---

/**
 * Renders menu item categories as filter chips.
 * @param {Array} categories - Array of category objects from Firebase.
 */
function renderCategories(categories) {
    categoryListEl.innerHTML = ''; // Clear existing
    const allChip = createChip('All', activeCategory === 'All');
    categoryListEl.appendChild(allChip);

    categories.forEach(cat => {
        const chip = createChip(cat.name, activeCategory === cat.name);
        categoryListEl.appendChild(chip);
    });
}

/**
 * Helper to create a single category filter chip.
 * @param {string} name - The category name.
 * @param {boolean} isActive - Whether the chip should be active.
 * @returns {HTMLElement} The created chip element.
 */
function createChip(name, isActive) {
    const chip = document.createElement('div');
    chip.className = `chip ${isActive ? 'active' : ''}`;
    chip.textContent = name;
    chip.addEventListener('click', () => setCategory(name));
    return chip;
}

/**
 * Sets the active category and re-renders the menu.
 * @param {string} name - The category name to filter by.
 */
function setCategory(name) {
    activeCategory = name;
    // Update active class on chips
    document.querySelectorAll('#category-list .chip').forEach(c => {
        c.classList.toggle('active', c.textContent === name);
    });
    renderMenu();
}

/**
 * Renders the menu items based on the active category.
 */
function renderMenu() {
    menuGridEl.innerHTML = ''; // Clear existing items
    const fragment = document.createDocumentFragment();
    const filteredMenu = activeCategory === "All" ?
        fullMenu :
        fullMenu.filter(i => i.category === activeCategory);

    filteredMenu.forEach(item => {
        if (item.isAvailable === false) return;
        const card = createMenuItemCard(item);
        fragment.appendChild(card);
    });

    menuGridEl.appendChild(fragment);
}

/**
 * Creates a DOM element for a single menu item.
 * @param {object} item - The menu item data.
 * @returns {HTMLElement} The menu card element.
 */
function createMenuItemCard(item) {
    const card = document.createElement('div');
    card.className = 'menu-card';

    const defImg = "https://img.freepik.com/free-vector/interior-hotel-room-with-bed-window-sketch_107791-3048.jpg";

    card.innerHTML = `
        <img src="${item.imageUrl || defImg}" class="item-img" alt="${item.name}">
        <div class="item-details">
            <div class="item-name">${item.name}</div>
            <div class="item-desc">${item.description || 'Delicious freshly prepared'}</div>
            <div class="item-price">₹${item.price}</div>
        </div>
    `;

    const addButton = document.createElement('button');
    addButton.className = 'btn-add';
    addButton.textContent = '+';
    addButton.addEventListener('click', () => addToCart(item));

    card.appendChild(addButton);
    return card;
}

/**
 * Adds an item to the cart or increments its quantity.
 * @param {object} item - The item to add.
 */
function addToCart(item) {
    if (cart[item.id]) {
        cart[item.id].quantity++;
    } else {
        cart[item.id] = {
            menuItemId: item.id,
            name: item.name,
            price: item.price,
            quantity: 1
        };
    }
    updateCartView();
}

/**
 * Updates the floating cart view with current totals.
 */
function updateCartView() {
    const items = Object.values(cart);
    if (items.length === 0) {
        cartSheet.style.display = 'none';
        return;
    }

    const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
    const totalPrice = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);

    itemCountEl.textContent = `${totalItems} Item(s)`;
    totalPriceEl.textContent = `₹${totalPrice.toLocaleString()}`;
    cartSheet.style.display = 'flex';
}

/**
 * Places the final order to Firebase.
 */
function placeOrder() {
    if (Object.keys(cart).length === 0) {
        alert("Your cart is empty.");
        return;
    }

    const orderId = `ORD_${Date.now()}`;
    const totalAmount = Object.values(cart).reduce((sum, item) => sum + (item.price * item.quantity), 0);

    const orderData = {
        id: orderId,
        roomNumber: roomNum,
        items: cart, // Send the cart object
        totalAmount: totalAmount,
        status: 'PENDING',
        timestamp: Date.now()
    };

    db.ref(`orders/${orderId}`).set(orderData)
        .then(() => {
            alert("Order Placed Successfully!");
            cart = {};
            updateCartView();
        })
        .catch(error => {
            console.error("Order failed: ", error);
            alert("There was an issue placing your order. Please try again.");
        });
}

/**
 * Sends a 'call staff' request to Firebase.
 */
function callStaff() {
    const callId = `CALL_${Date.now()}`;
    db.ref(`calls/${callId}`).set({
        id: callId,
        roomNumber: roomNum,
        status: 'PENDING',
        timestamp: Date.now()
    });
    callBtn.classList.add('requested');
    callBtn.innerHTML = "✅ STAFF NOTIFIED";
    callBtn.disabled = true; // Prevent multiple calls
}

// --- EVENT LISTENERS & INITIAL LOAD ---

function initializeApp() {
    roomNumLabel.textContent = `Room ${roomNum}`;

    // Attach event listeners
    callBtn.addEventListener('click', callStaff);
    orderBtn.addEventListener('click', placeOrder);

    // Firebase listeners
    db.ref('menu/categories').on('value', snap => {
        const categories = [];
        snap.forEach(c => categories.push(c.val()));
        renderCategories(categories);
    });

    db.ref('menu/items').on('value', snap => {
        fullMenu = [];
        snap.forEach(c => fullMenu.push(c.val()));
        renderMenu();
    });
}

// Run the app when the DOM is ready
document.addEventListener('DOMContentLoaded', initializeApp);