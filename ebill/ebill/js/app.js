// Theme Management
const themeToggleBtn = document.getElementById('theme-toggle-btn');
const rootBody = document.body;

// Load preferred theme on startup
const currentTheme = localStorage.getItem('theme') || 'dark';
if (currentTheme === 'light') {
    rootBody.classList.add('light-mode');
} else {
    rootBody.classList.remove('light-mode');
}

themeToggleBtn.addEventListener('click', () => {
    rootBody.classList.toggle('light-mode');
    const theme = rootBody.classList.contains('light-mode') ? 'light' : 'dark';
    localStorage.setItem('theme', theme);
    // Update theme toggle icon
    updateThemeIcon(theme);
});

function updateThemeIcon(theme) {
    const icon = themeToggleBtn.querySelector('i');
    if (!icon) return;
    if (theme === 'light') {
        icon.className = 'fas fa-moon';
    } else {
        icon.className = 'fas fa-sun';
    }
}

// Initial icon setup
updateThemeIcon(localStorage.getItem('theme') || 'dark');

// Input Mode (Units vs Readings) Toggling
const tabUnits = document.getElementById('tab-units');
const tabReadings = document.getElementById('tab-readings');
const inputTypeField = document.getElementById('inputType');
const unitsGroup = document.getElementById('group-units');
const readingsGroup = document.getElementById('group-readings');

if (tabUnits && tabReadings) {
    tabUnits.addEventListener('click', () => {
        tabUnits.classList.add('active');
        tabReadings.classList.remove('active');
        inputTypeField.value = 'units';
        unitsGroup.classList.remove('hidden-section');
        readingsGroup.classList.add('hidden-section');
        
        // Remove required attribute from input fields to prevent validation issues when hidden
        document.getElementById('prevReading').removeAttribute('required');
        document.getElementById('currReading').removeAttribute('required');
        document.getElementById('units').setAttribute('required', 'required');
    });

    tabReadings.addEventListener('click', () => {
        tabReadings.classList.add('active');
        tabUnits.classList.remove('active');
        inputTypeField.value = 'readings';
        readingsGroup.classList.remove('hidden-section');
        unitsGroup.classList.add('hidden-section');
        
        // Toggle required attributes
        document.getElementById('units').removeAttribute('required');
        document.getElementById('prevReading').setAttribute('required', 'required');
        document.getElementById('currReading').setAttribute('required', 'required');
    });
}

// Handle dynamic restore based on form model after submission
window.addEventListener('DOMContentLoaded', () => {
    // Sync calculations from JSP to client state via DOM bridge
    const bridge = document.getElementById('bill-bridge');
    if (bridge) {
        window.lastCalculatedBill = {
            customerName: bridge.getAttribute('data-customer'),
            meterNo: bridge.getAttribute('data-meter'),
            month: bridge.getAttribute('data-month'),
            units: parseFloat(bridge.getAttribute('data-units')),
            billAmount: parseFloat(bridge.getAttribute('data-amount')),
            timestamp: new Date().toLocaleDateString(undefined, {month: 'short', day: 'numeric', hour: '2-digit', minute:'2-digit'})
        };
    }

    // Restore input field selections from data-attributes to prevent custom JSP tags inside HTML markup
    const form = document.getElementById('bill-form');
    if (form) {
        const connVal = form.getAttribute('data-conn');
        const monthVal = form.getAttribute('data-month');
        const discountVal = form.getAttribute('data-discount');
        
        if (connVal) {
            const connSelect = document.getElementById('connType');
            if (connSelect) connSelect.value = connVal;
        }
        if (monthVal) {
            const monthSelect = document.getElementById('month');
            if (monthSelect) monthSelect.value = monthVal;
        }
        if (discountVal === 'true') {
            const discountCheck = document.getElementById('digitalDiscount');
            if (discountCheck) discountCheck.checked = true;
        }
    }

    const currentMode = inputTypeField ? inputTypeField.value : 'units';
    if (currentMode === 'readings' && tabReadings) {
        tabReadings.click();
    } else if (tabUnits) {
        tabUnits.click();
    }
    
    // Save generated bill details to history
    saveBillToHistory();
    
    // Render calculation history list
    renderHistoryList();
});

// Bill History Logic
function saveBillToHistory() {
    if (window.lastCalculatedBill) {
        let history = JSON.parse(localStorage.getItem('ebill_history') || '[]');
        
        // Avoid duplicate entries (e.g. from consecutive refreshes)
        const isDuplicate = history.some(item => 
            item.customerName === window.lastCalculatedBill.customerName &&
            item.meterNo === window.lastCalculatedBill.meterNo &&
            item.month === window.lastCalculatedBill.month &&
            item.units === window.lastCalculatedBill.units &&
            item.billAmount === window.lastCalculatedBill.billAmount
        );
        
        if (!isDuplicate) {
            history.unshift(window.lastCalculatedBill);
            // Limit to last 6 entries
            if (history.length > 6) history.pop();
            localStorage.setItem('ebill_history', JSON.stringify(history));
        }
    }
}

function renderHistoryList() {
    const historyContainer = document.getElementById('history-grid');
    if (!historyContainer) return;
    
    const history = JSON.parse(localStorage.getItem('ebill_history') || '[]');
    
    if (history.length === 0) {
        document.getElementById('history-section').style.display = 'none';
        return;
    }
    
    document.getElementById('history-section').style.display = 'block';
    historyContainer.innerHTML = '';
    
    history.forEach(item => {
        const card = document.createElement('div');
        card.className = 'history-card glass-card';
        card.innerHTML = `
            <div class="history-card-header">
                <span class="history-name">${escapeHTML(item.customerName)}</span>
                <span class="history-date">${item.timestamp}</span>
            </div>
            <div style="font-size: 0.75rem; color: var(--text-secondary-dark); margin-top: -4px;">
                Meter: ${escapeHTML(item.meterNo)} | ${escapeHTML(item.month)}
            </div>
            <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-top: 5px;">
                <span style="font-size: 0.85rem; font-weight: 500;">${item.units.toFixed(1)} Units</span>
                <span class="history-bill">₹${item.billAmount.toFixed(2)}</span>
            </div>
        `;
        historyContainer.appendChild(card);
    });
}

function clearHistory() {
    if (confirm('Are you sure you want to clear all calculation history?')) {
        localStorage.removeItem('ebill_history');
        renderHistoryList();
    }
}

function escapeHTML(str) {
    return str.replace(/[&<>'"]/g, 
        tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag)
    );
}

// Print Bill Function
function printBill() {
    window.print();
}

// Dynamic Client Validation
const billForm = document.getElementById('bill-form');
if (billForm) {
    billForm.addEventListener('submit', (e) => {
        const mode = inputTypeField.value;
        if (mode === 'readings') {
            const prev = parseFloat(document.getElementById('prevReading').value);
            const curr = parseFloat(document.getElementById('currReading').value);
            if (curr < prev) {
                e.preventDefault();
                alert('Validation Error: Current Reading must be greater than or equal to Previous Reading.');
            }
        } else {
            const units = parseFloat(document.getElementById('units').value);
            if (units < 0) {
                e.preventDefault();
                alert('Validation Error: Units Consumed cannot be negative.');
            }
        }
    });
}
