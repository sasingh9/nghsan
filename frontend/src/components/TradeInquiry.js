import React, { useState } from 'react';
import { DataGrid } from '@mui/x-data-grid';
import { Button, TextField, Typography, Box, Modal, Paper } from '@mui/material';
import JsonDataViewer from './JsonDataViewer';

const TradeInquiry = () => {
    const [clientRef, setClientRef] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedJson, setSelectedJson] = useState(null);

    const handleViewJson = (json) => {
        setSelectedJson(json);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedJson(null);
    };

    const columns = [
        { field: 'clientReferenceNumber', headerName: 'Client Ref', width: 150 },
        { field: 'fundNumber', headerName: 'Fund', width: 100 },
    { field: 'securityId', headerName: 'Security ID', width: 150 },
    { field: 'tradeDate', headerName: 'Trade Date', width: 150 },
    { field: 'settleDate', headerName: 'Settle Date', width: 150 },
    { field: 'quantity', headerName: 'Quantity', width: 120 },
    { field: 'price', headerName: 'Price', width: 120 },
        { field: 'principal', headerName: 'Principal', width: 120 },
        { field: 'netAmount', headerName: 'Net Amount', width: 120 },
        {
            field: 'actions',
            headerName: '...',
            width: 80,
            renderCell: (params) => (
                <Button
                    variant="outlined"
                    size="small"
                    onClick={() => handleViewJson(params.row.outboundJson)}
                >
                    ...
                </Button>
            ),
        },
    ];

    const handleSubmit = async (event) => {
        event.preventDefault();
        const hasClientRef = clientRef.trim();
        const hasDateRange = startDate && endDate;

        if (!hasClientRef && !hasDateRange) {
            setError('Please enter a Client Reference Number or select a date range.');
            return;
        }
        if (hasDateRange && startDate >= endDate) {
            setError('Start date must be before end date.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const params = new URLSearchParams();
            if (hasClientRef) {
                params.append('clientReferenceNumber', clientRef);
            }
            if (hasDateRange) {
                params.append('startDate', startDate);
                params.append('endDate', endDate);
            }

            const response = await fetch(`/api/trades?${params.toString()}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            const data = await response.json();
            if (response.ok && data.success) {
                // Add a unique id to each row for the DataGrid
                const tradesWithIds = data.data.map((trade, index) => ({ ...trade, id: index }));
                setTrades(tradesWithIds);
            } else {
                throw new Error(data.message || 'Network response was not ok');
            }
        } catch (error) {
            setError(error.message);
            setTrades([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <Typography variant="h4" gutterBottom>Trade Inquiry</Typography>
            <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
                <TextField
                    label="Client Reference Number"
                    value={clientRef}
                    onChange={(e) => setClientRef(e.target.value)}
                    variant="outlined"
                    size="small"
                    sx={{ minWidth: '240px' }}
                />
                <TextField
                    label="Start Date"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    size="small"
                />
                <TextField
                    label="End Date"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
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
                    getRowId={(row) => row.id}
                    slots={{
                      noRowsOverlay: () => (
                        <Box sx={{ p: 2, textAlign: 'center' }}>
                          <Typography>No trades found.</Typography>
                        </Box>
                      ),
                    }}
                />
            </div>

            <Modal
                open={isModalOpen}
                onClose={handleCloseModal}
                aria-labelledby="json-viewer-modal-title"
            >
                <Paper sx={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: '80%', maxHeight: '90vh', overflow: 'auto', p: 4 }}>
                    <Typography id="json-viewer-modal-title" variant="h6" component="h2" gutterBottom>
                        Outbound JSON Message
                    </Typography>
                    <JsonDataViewer data={selectedJson} />
                    <Button onClick={handleCloseModal} sx={{ mt: 2 }}>Close</Button>
                </Paper>
            </Modal>
        </div>
    );
};

export default TradeInquiry;
