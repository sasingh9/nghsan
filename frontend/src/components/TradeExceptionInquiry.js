import React, { useState } from 'react';

function TradeExceptionInquiry() {
    const [clientReference, setClientReference] = useState('');
    const [exceptions, setExceptions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError('');
        setExceptions([]);

        if (!clientReference) {
            setError('Please enter a Client Reference Number.');
            setLoading(false);
            return;
        }

        try {
            const response = await fetch(`/api/exceptions/${clientReference}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            setExceptions(data);
        } catch (e) {
            setError('Failed to fetch trade exceptions. ' + e.message);
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Trade Exception Inquiry</h2>
            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={clientReference}
                    onChange={(e) => setClientReference(e.target.value)}
                    placeholder="Enter Client Reference Number"
                    style={{ marginRight: '10px' }}
                />
                <button type="submit" disabled={loading}>
                    {loading ? 'Loading...' : 'Fetch Exceptions'}
                </button>
            </form>

            {error && <p className="error-message" style={{ color: 'red' }}>{error}</p>}

            {exceptions.length > 0 ? (
                <table style={{ marginTop: '20px', width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                        <tr>
                            <th style={tableHeaderStyle}>ID</th>
                            <th style={tableHeaderStyle}>Client Reference</th>
                            <th style={tableHeaderStyle}>Failure Reason</th>
                            <th style={tableHeaderStyle}>Created At</th>
                            <th style={tableHeaderStyle}>Failed JSON</th>
                        </tr>
                    </thead>
                    <tbody>
                        {exceptions.map((ex) => (
                            <tr key={ex.id}>
                                <td style={tableCellStyle}>{ex.id}</td>
                                <td style={tableCellStyle}>{ex.clientReferenceNumber}</td>
                                <td style={tableCellStyle}>{ex.failureReason}</td>
                                <td style={tableCellStyle}>{new Date(ex.createdAt).toLocaleString()}</td>
                                <td style={tableCellStyle}>
                                    <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                                        {JSON.stringify(JSON.parse(ex.failedTradeJson), null, 2)}
                                    </pre>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            ) : (
                <p>{!loading && 'No exceptions to display.'}</p>
            )}
        </div>
    );
}

const tableHeaderStyle = {
    border: '1px solid #ddd',
    padding: '8px',
    textAlign: 'left',
    backgroundColor: '#f2f2f2',
};

const tableCellStyle = {
    border: '1px solid #ddd',
    padding: '8px',
    textAlign: 'left',
};

export default TradeExceptionInquiry;
