import React, { useState } from 'react';
import { DataGrid } from '@mui/x-data-grid';
import { Button, TextField, Typography, Box } from '@mui/material';

const columns = [
    { field: 'clientReferenceNumber', headerName: 'Client Ref', width: 150 },
    { field: 'fundNumber', headerName: 'Fund', width: 100 },
    { field: 'securityId', headerName: 'Security ID', width: 150 },
    { field: 'tradeDate', headerName: 'Trade Date', width: 150 },
    { field: 'settleDate', headerName: 'Settle Date', width: 150 },
    { field: 'quantity', headerName: 'Quantity', width: 120 },
    { field: 'price', headerName: 'Price', width: 120 },
    { field: 'principal', headerName: 'Principal', width: 120 },
    { field: 'netAmount', headerName: 'Net Amount', width: 120 }
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
            const response = await fetch(`/api/trades/${clientRef}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
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
            <Typography variant="h4" gutterBottom>Trade Inquiry</Typography>
            <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                    label="Client Reference Number"
                    value={clientRef}
                    onChange={(e) => setClientRef(e.target.value)}
                    variant="outlined"
                    size="small"
                />
                <Button type="submit" variant="contained">Search</Button>
            </Box>

            {error && <Typography color="error">{error}</Typography>}

            <div style={{ height: 600, width: '100%' }}>
                <DataGrid
                    rows={trades}
                    columns={columns}
                    pageSize={10}
                    rowsPerPageOptions={[10]}
                    loading={loading}
                    getRowId={(row) => row.clientReferenceNumber + row.fundNumber + row.securityId} // Assuming this combination is unique
                />
            </div>
        </div>
    );
};

export default TradeInquiry;
