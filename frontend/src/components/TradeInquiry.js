import React, { useState } from 'react';
import DataGrid from 'react-data-grid';
import 'react-data-grid/lib/styles.css';

const columns = [
    { key: 'clientReferenceNumber', name: 'Client Ref' },
    { key: 'fundNumber', name: 'Fund' },
    { key: 'securityId', name: 'Security ID' },
    { key: 'tradeDate', name: 'Trade Date' },
    { key: 'settleDate', name: 'Settle Date' },
    { key: 'quantity', name: 'Quantity' },
    { key: 'price', name: 'Price' },
    { key: 'principal', name: 'Principal' },
    { key: 'netAmount', name: 'Net Amount' }
];

const TradeInquiry = () => {
    const [clientRef, setClientRef] = useState('');
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (!clientRef.trim()) {
            setError('Please enter a Client Reference Number.');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const headers = new Headers();
            headers.append('X-Correlation-ID', 'jules-debug-session');
            headers.append('X-Source-Application-ID', 'frontend');
            headers.append('Authorization', 'Basic ' + btoa('user:password'));

            const response = await fetch(`/api/trades/${clientRef}`, {
                method: 'GET',
                headers: headers,
                credentials: 'include'
            });
            const data = await response.json();
            if (response.ok && data.success) {
                setTrades(data.data);
            } else {
                throw new Error(data.message || 'Network response was not ok');
            }
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Trade Inquiry</h2>
            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={clientRef}
                    onChange={(e) => setClientRef(e.target.value)}
                    placeholder="Enter Client Reference Number"
                />
                <button type="submit">Search</button>
            </form>

            {loading && <p>Loading...</p>}
            {error && <p>Error: {error}</p>}

            <DataGrid columns={columns} rows={trades} />
        </div>
    );
};

export default TradeInquiry;
