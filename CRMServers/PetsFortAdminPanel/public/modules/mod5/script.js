async function initMod5() {
  const $ = id => document.getElementById(id);
  let products = [];
  let groups = [];
  let filtered = [];
  let editingGroupId = null;
  const selected = new Set();

  const els = {
    name: $('offer-group-name'), description: $('offer-description'), buy: $('offer-buy-qty'), free: $('offer-free-qty'),
    search: $('offer-product-search'), category: $('offer-category-filter'), status: $('offer-status-filter'), stock: $('offer-stock-filter'),
    selectAll: $('offer-select-all'), productList: $('offer-product-list'), selectedCount: $('offer-selected-count'),
    filterCount: $('offer-filter-count'), chips: $('offer-selected-chips'), summary: $('offer-selection-summary'),
    groupList: $('offer-group-list'), apply: $('offer-create-apply'),
    standaloneList: $('standalone-offer-list'), standaloneCount: $('standalone-offer-count'), standaloneSearch: $('standalone-offer-search')
  };

  const categories = StorageHelper.load('categories', []);
  categories.forEach(category => {
    const option = document.createElement('option'); option.value = category.id; option.textContent = category.name;
    els.category.appendChild(option);
  });
  const categoryName = id => categories.find(c => c.id === id)?.name || 'Uncategorized';
  const productById = id => products.find(p => p.product_id === id);

  function applyFilters() {
    const term = els.search.value.trim().toLowerCase();
    filtered = products.filter(p => {
      const haystack = `${p.product_name || ''} ${p.product_cid || ''} ${p.product_hsn || ''}`.toLowerCase();
      if (term && !haystack.includes(term)) return false;
      if (els.category.value && p.cat_id !== els.category.value) return false;
      if (els.status.value === 'active' && !p.offer_active) return false;
      if (els.status.value === 'none' && p.offer_active) return false;
      if (els.stock.value === 'in' && Number(p.stock) <= 0) return false;
      if (els.stock.value === 'out' && Number(p.stock) > 0) return false;
      return true;
    });
    renderProducts();
  }

  function renderProducts() {
    els.filterCount.textContent = `${filtered.length} matching`;
    els.selectAll.checked = filtered.length > 0 && filtered.every(p => selected.has(p.product_id));
    els.selectAll.indeterminate = filtered.some(p => selected.has(p.product_id)) && !els.selectAll.checked;
    if (!filtered.length) { els.productList.innerHTML = '<p class="empty-state">No products match these filters.</p>'; return; }
    els.productList.innerHTML = filtered.map(p => {
      const image = Array.isArray(p.product_img) ? p.product_img[0] : '';
      return `<label class="offer-product-row ${selected.has(p.product_id) ? 'selected' : ''}" data-id="${escapeHtml(p.product_id)}">
        <input type="checkbox" ${selected.has(p.product_id) ? 'checked' : ''}>
        ${image ? `<img src="${escapeHtml(image)}" alt="">` : '<span></span>'}
        <span class="offer-product-main"><strong>${escapeHtml(p.product_name)}</strong><small>${escapeHtml(p.product_cid || p.product_id)} · ${escapeHtml(categoryName(p.cat_id))}</small></span>
        <span class="offer-product-meta">Stock ${Number(p.stock) || 0}<br>${p.offer_active ? `<span class="offer-pill">Buy ${p.offer_buy_qty} + ${p.offer_free_qty}</span>` : 'No offer'}</span>
      </label>`;
    }).join('');
    els.productList.querySelectorAll('.offer-product-row').forEach(row => row.addEventListener('change', event => {
      const id = row.dataset.id; event.target.checked ? selected.add(id) : selected.delete(id); updateSelection(); renderProducts();
    }));
  }

  function updateSelection() {
    els.selectedCount.textContent = selected.size;
    els.summary.textContent = selected.size ? `${selected.size} products ready for this group` : 'No products selected';
    els.chips.innerHTML = [...selected].slice(0, 30).map(id => {
      const p = productById(id); return `<button class="selected-chip" data-id="${escapeHtml(id)}" title="Remove">${escapeHtml(p?.product_name || id)} ×</button>`;
    }).join('') + (selected.size > 30 ? `<span class="selected-chip">+${selected.size - 30} more</span>` : '');
    els.chips.querySelectorAll('button').forEach(button => button.onclick = () => { selected.delete(button.dataset.id); updateSelection(); renderProducts(); });
  }

  function payload() {
    return { name: els.name.value.trim(), description: els.description.value.trim(), buy_qty: Number(els.buy.value),
      free_qty: Number(els.free.value), product_ids: [...selected] };
  }

  function validate() {
    if (!els.name.value.trim()) return 'Enter a group name.';
    if (Number(els.buy.value) < 1 || Number(els.free.value) < 1) return 'Buy and free quantities must be at least 1.';
    if (!selected.size) return 'Select at least one product.';
    return '';
  }

  function resetBuilder() {
    editingGroupId = null; selected.clear(); els.name.value = ''; els.description.value = ''; els.buy.value = 10; els.free.value = 1;
    els.apply.innerHTML = '<i data-feather="send"></i> Create group & activate'; updateSelection(); applyFilters(); feather.replace();
  }

  async function createAndApply() {
    const error = validate(); if (error) { showToast(error, 'warning'); return; }
    showLoading(); els.apply.disabled = true;
    try {
      let group;
      if (editingGroupId) group = await callApi('PUT', `offer-groups/${editingGroupId}`, payload());
      else group = await callApi('POST', 'offer-groups', payload());
      await callApi('POST', `offer-groups/${group.id}/apply`, {});
      showToast(`Offer activated on ${selected.size} products. One notification was sent.`, 'success', 5000);
      resetBuilder(); await loadData();
    } catch (error) { showToast(`Could not activate group: ${error.message}`, 'error', 6000); }
    finally { hideLoading(); els.apply.disabled = false; }
  }

  function renderGroups() {
    if (!groups.length) { els.groupList.innerHTML = '<p class="empty-state">No managed offer groups yet.</p>'; return; }
    els.groupList.innerHTML = groups.map(g => `<article class="offer-group-card ${g.status === 'ACTIVE' ? 'active' : ''}">
      <div class="group-top"><div><h4>${escapeHtml(g.name)}</h4><div class="group-meta">${g.product_ids.length} products</div></div><span class="group-status ${g.status === 'ACTIVE' ? 'active' : ''}">${escapeHtml(g.status)}</span></div>
      <div class="group-rule">Buy ${g.buy_qty} · Get ${g.free_qty} free</div>
      <div class="group-meta">${escapeHtml(g.description || 'No custom notification message')}</div>
      <div class="group-actions">
        ${g.status === 'ACTIVE' ? `<button class="btn btn-warning btn-sm group-cancel" data-id="${g.id}">Cancel offer</button>` : `<button class="btn btn-outline btn-sm group-edit" data-id="${g.id}">Edit & reactivate</button>`}
        ${g.status === 'CANCELED' ? `<button class="btn btn-danger btn-sm group-delete" data-id="${g.id}">Delete group</button>` : ''}
      </div></article>`).join('');
    els.groupList.querySelectorAll('.group-cancel').forEach(b => b.onclick = () => cancelGroup(b.dataset.id));
    els.groupList.querySelectorAll('.group-delete').forEach(b => b.onclick = () => deleteGroup(b.dataset.id));
    els.groupList.querySelectorAll('.group-edit').forEach(b => b.onclick = () => editGroup(b.dataset.id));
  }

  function renderStandaloneOffers() {
    const term = els.standaloneSearch.value.trim().toLowerCase();
    const standalone = products.filter(product => {
      if (!product.offer_active || product.offer_group_id) return false;
      const haystack = `${product.product_name || ''} ${product.product_cid || ''} ${product.product_id || ''}`.toLowerCase();
      return !term || haystack.includes(term);
    });
    const total = products.filter(product => product.offer_active && !product.offer_group_id).length;
    els.standaloneCount.textContent = `${total} ${total === 1 ? 'offer' : 'offers'}`;
    if (!standalone.length) {
      els.standaloneList.innerHTML = `<p class="empty-state">${term ? 'No standalone offers match this search.' : 'No standalone product offers are active.'}</p>`;
      return;
    }
    els.standaloneList.innerHTML = standalone.map(product => {
      const image = Array.isArray(product.product_img) ? product.product_img[0] : '';
      return `<article class="standalone-offer-card">
        ${image ? `<img src="${escapeHtml(image)}" alt="">` : '<span class="standalone-offer-image"></span>'}
        <div class="standalone-offer-main"><strong>${escapeHtml(product.product_name)}</strong><small>${escapeHtml(product.product_cid || product.product_id)} - ${escapeHtml(categoryName(product.cat_id))}</small></div>
        <div class="standalone-offer-bottom"><span class="standalone-rule">Buy ${Number(product.offer_buy_qty)} - Get ${Number(product.offer_free_qty)} free</span><button class="btn btn-outline btn-sm standalone-edit" data-id="${escapeHtml(product.product_id)}">Edit product</button></div>
      </article>`;
    }).join('');
    els.standaloneList.querySelectorAll('.standalone-edit').forEach(button => button.onclick = () => {
      sessionStorage.setItem('admin-edit-product-id', button.dataset.id);
      loadModuleByName('Products');
    });
  }

  function editGroup(id) {
    const group = groups.find(g => g.id === id); if (!group) return;
    editingGroupId = id; selected.clear(); group.product_ids.forEach(pid => selected.add(pid));
    els.name.value = group.name; els.description.value = group.description || ''; els.buy.value = group.buy_qty; els.free.value = group.free_qty;
    els.apply.textContent = 'Save changes & reactivate'; updateSelection(); applyFilters(); window.scrollTo({top:0,behavior:'smooth'});
  }

  async function cancelGroup(id) {
    const group = groups.find(g => g.id === id); if (!confirm(`Cancel "${group?.name}" and clear its offer from all owned products?`)) return;
    showLoading(); try { await callApi('POST', `offer-groups/${id}/cancel`, {}); showToast('Group offer canceled.', 'success'); await loadData(); }
    catch (e) { showToast(`Cancel failed: ${e.message}`, 'error'); } finally { hideLoading(); }
  }

  async function deleteGroup(id) {
    const group = groups.find(g => g.id === id); if (!confirm(`Permanently delete the canceled group "${group?.name}"?`)) return;
    showLoading(); try { await callApi('DELETE', `offer-groups/${id}`); showToast('Group deleted.', 'success'); await loadData(); }
    catch (e) { showToast(`Delete failed: ${e.message}`, 'error'); } finally { hideLoading(); }
  }

  async function loadManualBanners() {
    const snapshot = await database.ref('datas/announcement/all').once('value'); const data = snapshot.val() || {};
    const entries = Object.entries(data).filter(([,value]) => typeof value === 'string');
    $('manual-banner-list').innerHTML = entries.map(([key,url]) => `<div class="manual-banner"><img src="${escapeHtml(url)}"><button class="btn btn-danger btn-sm" data-key="${escapeHtml(key)}">×</button></div>`).join('');
    $('manual-banner-list').querySelectorAll('button').forEach(b => b.onclick = async () => { await database.ref(`datas/announcement/all/${b.dataset.key}`).remove(); await loadManualBanners(); });
  }

  async function loadData() {
    [products, groups] = await Promise.all([
      callApi('POST', 'products/query', {filters:[],limit:10000,offset:0,order_by:'product_name',order_direction:'ASC'}),
      callApi('GET', 'offer-groups')
    ]);
    applyFilters(); renderGroups(); renderStandaloneOffers(); updateSelection(); feather.replace();
  }

  [els.search, els.category, els.status, els.stock].forEach(input => input.addEventListener(input.tagName === 'INPUT' ? 'input' : 'change', applyFilters));
  els.standaloneSearch.addEventListener('input', renderStandaloneOffers);
  els.selectAll.addEventListener('change', () => { filtered.forEach(p => els.selectAll.checked ? selected.add(p.product_id) : selected.delete(p.product_id)); updateSelection(); renderProducts(); });
  $('offer-clear-selection').onclick = () => { selected.clear(); updateSelection(); renderProducts(); };
  $('offer-refresh').onclick = loadData; els.apply.onclick = createAndApply;
  $('manual-banner-add').onclick = async () => { const input=$('manual-banner-url'),url=input.value.trim(); if(!isValidHttpUrl(url)){showToast('Enter a valid URL.','warning');return;} await database.ref(`datas/announcement/all/${Date.now()}`).set(url); input.value=''; await loadManualBanners(); };

  showLoading(); try { await Promise.all([loadData(), loadManualBanners()]); } catch(e) { showToast(`Could not load offers: ${e.message}`,'error',6000); } finally { hideLoading(); }
}
window.initMod5 = initMod5;
