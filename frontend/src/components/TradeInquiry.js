import React, { useState } from 'react';

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
            const response = await fetch(`/api/trades/${clientRef}`, { credentials: 'include' });
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

            <table>
                <thead>
                    <tr>
                        <th>Client Ref</th>
                        <th>Fund</th>
                        <th>Security ID</th>
                        <th>Trade Date</th>
                        <th>Settle Date</th>
                        <th>Quantity</th>
                        <th>Price</th>
                        <th>Principal</th>
                        <th>Net Amount</th>
                    </tr>
                </thead>
                <tbody>
                    {trades.map((trade, index) => (
                        <tr key={index}>
                            <td>{trade.clientReferenceNumber}</td>
                            <td>{trade.fundNumber}</td>
                            <td>{trade.securityId}</td>
                            <td>{trade.tradeDate}</td>
                            <td>{trade.settleDate}</td>
                            <td>{trade.quantity}</td>
                            <td>{trade.price}</td>
                            <td>{trade.principal}</td>
                            <td>{trade.netAmount}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default TradeInquiry;
