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
    <div style={{ fontFamily: 'sans-serif', padding: '1rem', maxWidth: 960, margin: '0 auto' }}>
      <h2>Ledgerly Domain Demo</h2>
      <p>Minimal UI for merchants, transactions, outcomes.</p>
      {error && <div style={{ color: 'red' }}>Error: {error}</div>}

      <section style={{ marginBottom: '1rem' }}>
        <h4>Create Merchant</h4>
        <input
          placeholder="id"
          value={merchantForm.id}
          onChange={(e) => setMerchantForm({ ...merchantForm, id: e.target.value })}
          style={{ marginRight: '0.5rem' }}
        />
        <input
          placeholder="name"
          value={merchantForm.name}
          onChange={(e) => setMerchantForm({ ...merchantForm, name: e.target.value })}
          style={{ marginRight: '0.5rem' }}
        />
        <input
          placeholder="status"
          value={merchantForm.status}
          onChange={(e) => setMerchantForm({ ...merchantForm, status: e.target.value })}
          style={{ marginRight: '0.5rem' }}
        />
        <button onClick={createMerchant}>Create</button>
      </section>

      <section style={{ marginBottom: '1rem' }}>
        <h4>Create Transaction</h4>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
          <input placeholder="id" value={txForm.id} onChange={(e) => setTxForm({ ...txForm, id: e.target.value })} />
          <input
            placeholder="merchantId"
            value={txForm.merchantId}
            onChange={(e) => setTxForm({ ...txForm, merchantId: e.target.value })}
          />
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
        <button onClick={createTransaction} style={{ marginTop: '0.5rem' }}>
          Create Transaction
        </button>
      </section>

      <section style={{ marginBottom: '1rem' }}>
        <h4>Transactions</h4>
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
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
          <button onClick={loadTransactions}>Refresh</button>
          <button onClick={expirePending}>Expire Pending</button>
        </div>
        <pre style={{ background: '#f5f5f5', padding: '0.5rem', minHeight: '150px' }}>
          {JSON.stringify(transactions, null, 2)}
        </pre>
      </section>

      <section>
        <h4>Assert Outcome</h4>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
          <input placeholder="txId" value={outcomeForm.txId} onChange={(e) => setOutcomeForm({ ...outcomeForm, txId: e.target.value })} />
          <input
            placeholder="status (SUCCESS/FAILED)"
            value={outcomeForm.status}
            onChange={(e) => setOutcomeForm({ ...outcomeForm, status: e.target.value })}
          />
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
          <input
            placeholder="metadata"
            value={outcomeForm.metadata}
            onChange={(e) => setOutcomeForm({ ...outcomeForm, metadata: e.target.value })}
          />
        </div>
        <button onClick={assertOutcome} style={{ marginTop: '0.5rem' }}>
          Assert Outcome
        </button>
      </section>
    </div>
  );
}

export default App;
