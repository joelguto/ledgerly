import React, { useEffect, useState } from 'react';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function App() {
  const [error, setError] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [merchantForm, setMerchantForm] = useState({ id: '', name: '', status: 'ACTIVE' });
  const [txForm, setTxForm] = useState({
    id: '',
    merchantId: '',
    amount: 0,
    currency: 'USD',
    expiresAt: '',
    metadata: '',
  });
  const [outcomeForm, setOutcomeForm] = useState({
    txId: '',
    status: 'SUCCESS',
    externalReference: '',
    reportedAt: '',
    metadata: '',
  });
  const [merchantFilter, setMerchantFilter] = useState('');
  const [stateFilter, setStateFilter] = useState('');

  const catchErr = (e) => setError(e.message);

  const createMerchant = () => {
    fetch(`${API_BASE}/ledger/merchants`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: merchantForm.id,
        name: merchantForm.name,
        status: merchantForm.status,
      }),
    })
      .then((r) => (r.ok ? r : r.json().then((x) => Promise.reject(new Error(x.error || 'Failed')))))
      .then(() => setError(''))
      .catch(catchErr);
  };

  const createTransaction = () => {
    fetch(`${API_BASE}/ledger/transactions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: txForm.id,
        merchantId: txForm.merchantId,
        amount: Number(txForm.amount),
        currency: txForm.currency,
        expiresAt: txForm.expiresAt || undefined,
        metadata: txForm.metadata || undefined,
      }),
    })
      .then((r) => (r.ok ? r : r.json().then((x) => Promise.reject(new Error(x.error || 'Failed')))))
      .then(() => {
        setError('');
        loadTransactions();
      })
      .catch(catchErr);
  };

  const loadTransactions = () => {
    const params = new URLSearchParams();
    if (merchantFilter) params.append('merchant_id', merchantFilter);
    if (stateFilter) params.append('state', stateFilter);
    fetch(`${API_BASE}/ledger/transactions?${params.toString()}`)
      .then((r) => r.json())
      .then((data) => {
        setTransactions(data);
        setError('');
      })
      .catch(catchErr);
  };

  const assertOutcome = () => {
    fetch(`${API_BASE}/ledger/transactions/${outcomeForm.txId}/outcome`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        status: outcomeForm.status,
        externalReference: outcomeForm.externalReference || undefined,
        reportedAt: outcomeForm.reportedAt || undefined,
        metadata: outcomeForm.metadata || undefined,
      }),
    })
      .then((r) => (r.ok ? r : r.json().then((x) => Promise.reject(new Error(x.error || 'Failed')))))
      .then(() => {
        setError('');
        loadTransactions();
      })
      .catch(catchErr);
  };

  const expirePending = () => {
    fetch(`${API_BASE}/ledger/transactions/expire`, { method: 'POST' })
      .then((r) => r.json())
      .then((data) => {
        setError('');
        loadTransactions();
        alert(`Expired: ${data.expired}`);
      })
      .catch(catchErr);
  };

  useEffect(() => {
    loadTransactions();
  }, []);

  return (
    <div className="app-shell">
      <div className="hero">
        <div>
          <h1>Ledgerly Domain Console</h1>
          <p>Retro controls for merchants, transactions, and outcomes.</p>
        </div>
        <span className="badge">Retro mode • Live</span>
      </div>

      {error && <div className="error">Error: {error}</div>}

      <div className="grid">
        <section className="card">
          <div className="section-title">
            <h3>Create Merchant</h3>
            <span className="pill">POST /ledger/merchants</span>
          </div>
          <div className="field-stack">
            <input
              placeholder="id"
              value={merchantForm.id}
              onChange={(e) => setMerchantForm({ ...merchantForm, id: e.target.value })}
            />
            <input
              placeholder="name"
              value={merchantForm.name}
              onChange={(e) => setMerchantForm({ ...merchantForm, name: e.target.value })}
            />
            <input
              placeholder="status (e.g., ACTIVE)"
              value={merchantForm.status}
              onChange={(e) => setMerchantForm({ ...merchantForm, status: e.target.value })}
            />
            <div className="controls">
              <button onClick={createMerchant}>Create merchant</button>
            </div>
          </div>
        </section>

        <section className="card">
          <div className="section-title">
            <h3>Create Transaction</h3>
            <span className="pill">POST /ledger/transactions</span>
          </div>
          <div className="field-stack">
            <div className="field-row">
              <input placeholder="id" value={txForm.id} onChange={(e) => setTxForm({ ...txForm, id: e.target.value })} />
              <input
                placeholder="merchantId"
                value={txForm.merchantId}
                onChange={(e) => setTxForm({ ...txForm, merchantId: e.target.value })}
              />
            </div>
            <div className="field-row">
              <input
                placeholder="amount (cents)"
                type="number"
                value={txForm.amount}
                onChange={(e) => setTxForm({ ...txForm, amount: e.target.value })}
              />
              <input
                placeholder="currency"
                value={txForm.currency}
                onChange={(e) => setTxForm({ ...txForm, currency: e.target.value })}
              />
            </div>
            <div className="field-row">
              <input
                placeholder="expiresAt (ISO optional)"
                value={txForm.expiresAt}
                onChange={(e) => setTxForm({ ...txForm, expiresAt: e.target.value })}
              />
              <input
                placeholder="metadata"
                value={txForm.metadata}
                onChange={(e) => setTxForm({ ...txForm, metadata: e.target.value })}
              />
            </div>
            <div className="controls">
              <button onClick={createTransaction}>Create transaction</button>
            </div>
          </div>
        </section>

        <section className="card">
          <div className="section-title">
            <h3>Assert Outcome</h3>
            <span className="pill">POST /ledger/transactions/:id/outcome</span>
          </div>
          <div className="field-stack">
            <div className="field-row">
              <input
                placeholder="txId"
                value={outcomeForm.txId}
                onChange={(e) => setOutcomeForm({ ...outcomeForm, txId: e.target.value })}
              />
              <input
                placeholder="status (SUCCESS/FAILED)"
                value={outcomeForm.status}
                onChange={(e) => setOutcomeForm({ ...outcomeForm, status: e.target.value })}
              />
            </div>
            <div className="field-row">
              <input
                placeholder="externalReference"
                value={outcomeForm.externalReference}
                onChange={(e) => setOutcomeForm({ ...outcomeForm, externalReference: e.target.value })}
              />
              <input
                placeholder="reportedAt (ISO optional)"
                value={outcomeForm.reportedAt}
                onChange={(e) => setOutcomeForm({ ...outcomeForm, reportedAt: e.target.value })}
              />
            </div>
            <input
              placeholder="metadata"
              value={outcomeForm.metadata}
              onChange={(e) => setOutcomeForm({ ...outcomeForm, metadata: e.target.value })}
            />
            <div className="controls">
              <button onClick={assertOutcome}>Assert outcome</button>
            </div>
          </div>
        </section>

        <section className="card">
          <div className="section-title">
            <h3>Filters & Actions</h3>
            <span className="pill">GET /ledger/transactions</span>
          </div>
          <div className="stack">
            <div className="filters">
              <input
                placeholder="merchant filter"
                value={merchantFilter}
                onChange={(e) => setMerchantFilter(e.target.value)}
              />
              <input
                placeholder="state filter (PENDING/...)"
                value={stateFilter}
                onChange={(e) => setStateFilter(e.target.value)}
              />
            </div>
            <div className="controls">
              <button className="secondary" onClick={loadTransactions}>
                Refresh
              </button>
              <button className="danger" onClick={expirePending}>
                Expire pending
              </button>
            </div>
          </div>
        </section>
      </div>

      <section className="card" style={{ marginTop: 16 }}>
        <div className="section-title">
          <h3>Transactions</h3>
          <span className="pill">Live data</span>
        </div>
        <div className="log-window">
          <pre>{JSON.stringify(transactions, null, 2)}</pre>
        </div>
      </section>

      <div className="footer-note">Ledgerly mini-ledger • Retro UI</div>
    </div>
  );
}

export default App;
