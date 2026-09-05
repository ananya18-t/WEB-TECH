// ElectroBill Servlet Script

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const billForm = document.getElementById('billForm');
  const customerName = document.getElementById('customerName');
  const consumerId = document.getElementById('consumerId');
  const category = document.getElementById('category');
  const billingMonth = document.getElementById('billingMonth');
  const unitsInput = document.getElementById('units');
  const includeFixedCharge = document.getElementById('includeFixedCharge');
  const taxPercent = document.getElementById('taxPercent');
  const btnGenId = document.getElementById('btnGenId');
  const btnAjaxCalc = document.getElementById('btnAjaxCalc');
  const formatInput = document.getElementById('formatInput');

  const resultEmpty = document.getElementById('resultEmpty');
  const resultContent = document.getElementById('resultContent');
  const resTotal = document.getElementById('resTotal');
  const resMeta = document.getElementById('resMeta');
  const resTableBody = document.getElementById('resTableBody');
  const resBaseAmt = document.getElementById('resBaseAmt');
  const resFixedAmt = document.getElementById('resFixedAmt');
  const resTaxAmt = document.getElementById('resTaxAmt');
  const resGrandAmt = document.getElementById('resGrandAmt');

  const barS1 = document.getElementById('barS1');
  const barS2 = document.getElementById('barS2');
  const barS3 = document.getElementById('barS3');
  const barS4 = document.getElementById('barS4');

  const btnPrint = document.getElementById('btnPrint');
  const btnCopyJson = document.getElementById('btnCopyJson');
  const historySection = document.getElementById('historySection');
  const historyGrid = document.getElementById('historyGrid');
  const btnClearHistory = document.getElementById('btnClearHistory');

  let currentBillData = null;

  // Generate random Consumer ID
  function generateConsumerId() {
    const randomNum = Math.floor(100000 + Math.random() * 900000);
    consumerId.value = `ELE-${randomNum}`;
  }

  // Pre-fill defaults if empty
  if (!consumerId.value) generateConsumerId();
  if (!customerName.value) customerName.value = 'Ananya Sharma';

  btnGenId.addEventListener('click', generateConsumerId);

  // Preset Buttons Handler
  document.querySelectorAll('.btn-preset').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const presetVal = e.target.getAttribute('data-preset');
      unitsInput.value = presetVal;
      showToast(`Selected preset: ${presetVal} kWh`);
      calculateBillAjax();
    });
  });

  // Calculate via Servlet (AJAX)
  btnAjaxCalc.addEventListener('click', (e) => {
    e.preventDefault();
    calculateBillAjax();
  });

  // Submit Form POST Handler
  billForm.addEventListener('submit', (e) => {
    if (!validateForm()) {
      e.preventDefault();
      return;
    }
    formatInput.value = 'html';
  });

  function validateForm() {
    if (!customerName.value.trim()) {
      showToast('Please enter consumer name', true);
      customerName.focus();
      return false;
    }
    if (!consumerId.value.trim()) {
      showToast('Please enter consumer ID', true);
      consumerId.focus();
      return false;
    }
    const units = parseFloat(unitsInput.value);
    if (isNaN(units) || units < 0) {
      showToast('Please enter a valid non-negative units value', true);
      unitsInput.focus();
      return false;
    }
    return true;
  }

  async function calculateBillAjax() {
    if (!validateForm()) return;

    const units = parseFloat(unitsInput.value);
    const params = new URLSearchParams({
      customerName: customerName.value.trim(),
      consumerId: consumerId.value.trim(),
      category: category.value,
      billingMonth: billingMonth.value,
      units: units,
      includeFixedCharge: includeFixedCharge.checked ? 'true' : 'false',
      taxPercent: taxPercent.value || '0',
      format: 'json'
    });

    try {
      showToast('Calculating via Servlet...');
      const response = await fetch(`ElectricityBillServlet?${params.toString()}`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        renderResults(data);
        saveToHistory(data);
        showToast('Bill calculated successfully via Servlet!');
      } else {
        const errData = await response.json();
        showToast(errData.message || 'Servlet error', true);
        fallbackClientCalculation();
      }
    } catch (err) {
      console.warn('Servlet API unreachable, using client-side Servlet logic mirror:', err);
      fallbackClientCalculation();
    }
  }

  // Pure Client Fallback mirroring exact Servlet BillCalculator logic
  function fallbackClientCalculation() {
    const units = parseFloat(unitsInput.value) || 0;
    const name = customerName.value.trim() || 'Valued Consumer';
    const cid = consumerId.value.trim() || 'ELE-123456';
    const cat = category.value;
    const month = billingMonth.value;
    const isFixed = includeFixedCharge.checked;
    const taxP = parseFloat(taxPercent.value) || 0;

    let rem = units;
    const breakdown = [];
    let baseAmount = 0;

    // Slab 1: 0 - 50 @ 3.50
    const u1 = Math.min(rem, 50);
    const amt1 = u1 * 3.50;
    breakdown.push({ slabName: 'Slab 1 (First 50 Units)', range: '1 - 50 Units', units: u1, rate: 3.50, amount: amt1 });
    baseAmount += amt1;
    rem = Math.max(0, rem - 50);

    // Slab 2: 51 - 150 @ 4.00
    const u2 = Math.min(rem, 100);
    const amt2 = u2 * 4.00;
    breakdown.push({ slabName: 'Slab 2 (Next 100 Units)', range: '51 - 150 Units', units: u2, rate: 4.00, amount: amt2 });
    baseAmount += amt2;
    rem = Math.max(0, rem - 100);

    // Slab 3: 151 - 250 @ 5.20
    const u3 = Math.min(rem, 100);
    const amt3 = u3 * 5.20;
    breakdown.push({ slabName: 'Slab 3 (Next 100 Units)', range: '151 - 250 Units', units: u3, rate: 5.20, amount: amt3 });
    baseAmount += amt3;
    rem = Math.max(0, rem - 100);

    // Slab 4: > 250 @ 6.50
    const u4 = rem;
    const amt4 = u4 * 6.50;
    breakdown.push({ slabName: 'Slab 4 (Above 250 Units)', range: '> 250 Units', units: u4, rate: 6.50, amount: amt4 });
    baseAmount += amt4;

    const fixedCharge = isFixed ? 50.00 : 0.00;
    const taxAmount = Math.round(((baseAmount + fixedCharge) * (taxP / 100.0)) * 100) / 100;
    const netPayable = Math.round((baseAmount + fixedCharge + taxAmount) * 100) / 100;

    const today = new Date();
    const dueDate = new Date();
    dueDate.setDate(today.getDate() + 15);

    const fallbackData = {
      customerName: name,
      consumerId: cid,
      category: cat,
      billingMonth: month,
      totalUnits: units,
      slabBreakdown: breakdown,
      baseAmount: Math.round(baseAmount * 100) / 100,
      fixedCharge: fixedCharge,
      taxPercentage: taxP,
      taxAmount: taxAmount,
      netPayable: netPayable,
      billDate: today.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
      dueDate: dueDate.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
      billNo: `INV-${Date.now() % 10000000}`
    };

    renderResults(fallbackData);
    saveToHistory(fallbackData);
    showToast('Bill calculated successfully!');
  }

  // Render Result UI
  function renderResults(data) {
    currentBillData = data;
    resultEmpty.style.display = 'none';
    resultContent.style.display = 'block';

    // Hero Total
    animateValue(resTotal, data.netPayable);
    resMeta.textContent = `Due Date: ${data.dueDate} | Bill #${data.billNo}`;

    // Slab Progress Bar
    const totalU = data.totalUnits || 1; // avoid divide by zero
    const s1Units = data.slabBreakdown[0] ? data.slabBreakdown[0].units : 0;
    const s2Units = data.slabBreakdown[1] ? data.slabBreakdown[1].units : 0;
    const s3Units = data.slabBreakdown[2] ? data.slabBreakdown[2].units : 0;
    const s4Units = data.slabBreakdown[3] ? data.slabBreakdown[3].units : 0;

    barS1.style.width = `${(s1Units / totalU) * 100}%`;
    barS2.style.width = `${(s2Units / totalU) * 100}%`;
    barS3.style.width = `${(s3Units / totalU) * 100}%`;
    barS4.style.width = `${(s4Units / totalU) * 100}%`;

    // Table Body
    resTableBody.innerHTML = '';
    data.slabBreakdown.forEach((slab, index) => {
      const tr = document.createElement('tr');
      const badgeClass = `badge-s${index + 1}`;
      tr.innerHTML = `
        <td><span class="badge-slab ${badgeClass}">${slab.range}</span></td>
        <td>${slab.units.toFixed(2)} kWh</td>
        <td>₹${slab.rate.toFixed(2)}</td>
        <td><strong>₹${slab.amount.toFixed(2)}</strong></td>
      `;
      resTableBody.appendChild(tr);
    });

    // Summary Rows
    resBaseAmt.textContent = `₹${data.baseAmount.toFixed(2)}`;
    resFixedAmt.textContent = `₹${data.fixedCharge.toFixed(2)}`;
    resTaxAmt.textContent = `₹${data.taxAmount.toFixed(2)} (${data.taxPercentage}%)`;
    resGrandAmt.textContent = `₹${data.netPayable.toFixed(2)}`;
  }

  // Count up animation
  function animateValue(element, finalVal) {
    const duration = 600;
    const startVal = 0;
    const startTime = performance.now();

    function step(currentTime) {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const current = startVal + progress * (finalVal - startVal);
      element.textContent = `₹${current.toFixed(2)}`;
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    }
    requestAnimationFrame(step);
  }

  // Copy JSON
  btnCopyJson.addEventListener('click', () => {
    if (!currentBillData) return;
    navigator.clipboard.writeText(JSON.stringify(currentBillData, null, 2))
      .then(() => showToast('Bill JSON copied to clipboard!'))
      .catch(() => showToast('Failed to copy', true));
  });

  // Print Receipt
  btnPrint.addEventListener('click', () => {
    window.print();
  });

  // Local Storage History
  function saveToHistory(data) {
    let history = JSON.parse(localStorage.getItem('electro_bill_history') || '[]');
    // Add to beginning, keep max 6
    history.unshift(data);
    history = history.slice(0, 6);
    localStorage.setItem('electro_bill_history', JSON.stringify(history));
    renderHistory();
  }

  function renderHistory() {
    const history = JSON.parse(localStorage.getItem('electro_bill_history') || '[]');
    if (history.length === 0) {
      historySection.style.display = 'none';
      return;
    }

    historySection.style.display = 'block';
    historyGrid.innerHTML = '';

    history.forEach(item => {
      const card = document.createElement('div');
      card.className = 'history-item';
      card.innerHTML = `
        <div class="h-header">
          <span>${item.customerName} (${item.consumerId})</span>
          <span>${item.billDate}</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: baseline;">
          <div>
            <strong style="color: var(--text-sub);">${item.totalUnits} kWh</strong>
            <span style="font-size: 0.8rem; color: var(--text-muted);">(${item.category})</span>
          </div>
          <div class="h-amount">₹${item.netPayable.toFixed(2)}</div>
        </div>
      `;
      card.addEventListener('click', () => renderResults(item));
      historyGrid.appendChild(card);
    });
  }

  btnClearHistory.addEventListener('click', () => {
    localStorage.removeItem('electro_bill_history');
    renderHistory();
    showToast('History cleared');
  });

  // Initial History Render
  renderHistory();

  // Toast Notification Helper
  function showToast(msg, isError = false) {
    const toast = document.getElementById('toast');
    const toastMsg = document.getElementById('toastMsg');
    toastMsg.textContent = msg;
    toast.style.borderColor = isError ? 'var(--accent-rose)' : 'var(--primary-cyan)';
    toast.classList.add('show');
    setTimeout(() => {
      toast.classList.remove('show');
    }, 3000);
  }
});
